package com.group_finity.mascot;

import java.awt.AWTException;
import java.awt.Point;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.image.ImagePairs;
import com.group_finity.mascot.imagesetchooser.ImageSetChooser;
import com.group_finity.mascot.sound.Sounds;
import com.joconner.i18n.Utf8ResourceBundleControl;
import com.nilo.plaf.nimrod.NimRODLookAndFeel;
import com.nilo.plaf.nimrod.NimRODTheme;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import javax.swing.JDialog;
import javax.swing.UIManager;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Program entry point.
 *
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Main
{
    private static final Logger log = Logger.getLogger( Main.class.getName() );
    // Action that matches the "Gather Around Mouse!" context menu command
    static final String BEHAVIOR_GATHER = "ChaseMouse";

    static
    {
        try
        {
            LogManager.getLogManager( ).readConfiguration( Main.class.getResourceAsStream( "/logging.properties" ) );
        }
        catch( final SecurityException e )
        {
            e.printStackTrace( );
        }
        catch( final IOException e )
        {
            e.printStackTrace( );
        }
        catch( OutOfMemoryError err )
        {
            log.log( Level.SEVERE, "Out of Memory Exception.  There are probably have too many "
                    + "Shimeji mascots in the image folder for your computer to handle.  Select fewer"
                    + " image sets or move some to the img/unused folder and try again.", err );
            Main.showError( "Out of Memory.  There are probably have too many \n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the \n"
                    + "img/unused folder and try again." );
            System.exit( 0 );
        }
    }
    private final Manager manager = new Manager( );
    private ArrayList<String> imageSets = new ArrayList<String>( );
    private ConcurrentHashMap<String, Configuration> configurations = new ConcurrentHashMap<String, Configuration>( );
    private ConcurrentHashMap<String, ArrayList<String>> childImageSets = new ConcurrentHashMap<String, ArrayList<String>>( );
    private static Main instance = new Main( );
    private Properties properties = new Properties( );
    private Platform platform;
    private ResourceBundle languageBundle;
    
    private JDialog form;
    
    public static Main getInstance( )
    {
        return instance;
    }
    private static JFrame frame = new javax.swing.JFrame( );

    public static void showError( String message )
    {
        JOptionPane.showMessageDialog( frame, message, "Error", JOptionPane.ERROR_MESSAGE );
    }
    
    public static void showError( String message, Throwable exception )
    {
        do
        {
            if( exception.getClass( ).getName( ).startsWith( "com.group_finity.mascot.exception" ) )
                message += "\n" + exception.getMessage( );
            else if( exception instanceof org.xml.sax.SAXParseException )
                message += "\n" + "Line " + ((org.xml.sax.SAXParseException)exception).getLineNumber( ) + ": " + exception.getMessage( );
            else
                message += "\n" + exception.toString( );
            exception = exception.getCause( );
        }
        while( exception != null );
        Main.showError( message + "\n" + instance.languageBundle.getString( "SeeLogForDetails" ) );
    }

    public static void main( final String[] args )
    {
        try
        {
            getInstance( ).run( );
        }
        catch( OutOfMemoryError err )
        {
            log.log( Level.SEVERE, "Out of Memory Exception.  There are probably have too many "
                    + "Shimeji mascots in the image folder for your computer to handle.  Select fewer"
                    + " image sets or move some to the img/unused folder and try again.", err );
            Main.showError( "Out of Memory.  There are probably have too many \n"
                    + "Shimeji mascots for your computer to handle.\n"
                    + "Select fewer image sets or move some to the \n"
                    + "img/unused folder and try again." );
            System.exit( 0 );
        }
    }

    public void run( )
    {   
        // test operating system
        if( !System.getProperty( "sun.arch.data.model" ).equals( "64" ) )
            platform = Platform.x86;
        else
            platform = Platform.x86_64;
        
        // load properties
        properties = new Properties( );
        FileInputStream input;
        try
        {
            input = new FileInputStream( Paths.get( "conf", "settings.properties" ).toString( ) );
            properties.load( input );
        }
        catch( FileNotFoundException ex )
        {
        }
        catch( IOException ex )
        {
        }
        
        // load languages
        try   
        {
            ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl( false );
            languageBundle = ResourceBundle.getBundle( "language", Locale.forLanguageTag( properties.getProperty( "Language", "en-GB" ) ), utf8Control );
        }
        catch( Exception ex )
        {
            Main.showError( "The default language file could not be loaded. Ensure that you have the latest shimeji language.properties in your conf directory." );
            exit( );
        }
        
        // load theme
        try
        {
            // default light theme
            NimRODLookAndFeel lookAndFeel = new NimRODLookAndFeel( );
            
            // check for theme properties
            NimRODTheme theme = null;
            try
            {
                if( new File( Paths.get( "conf", "theme.properties" ).toString( ) ).isFile( ) )
                {
                    theme = new NimRODTheme( Paths.get( "conf", "theme.properties" ).toString( ) );
                }
            }
            catch( Exception exc )
            {
                theme = null;
            }
            
            if( theme == null )
            {
                // default back to light theme if not found/valid
                theme = new NimRODTheme( );
                theme.setPrimary1( Color.decode( "#1EA6EB" ) );
                theme.setPrimary2( Color.decode( "#28B0F5" ) );
                theme.setPrimary3( Color.decode( "#32BAFF" ) );
                theme.setSecondary1( Color.decode( "#BCBCBE" ) );
                theme.setSecondary2( Color.decode( "#C6C6C8" ) );
                theme.setSecondary3( Color.decode( "#D0D0D2" ) );
                theme.setMenuOpacity( 255 );
                theme.setFrameOpacity( 255 );
            }
            
            // handle menu size
            if( !properties.containsKey( "MenuDPI" ) )
            {
                properties.setProperty( "MenuDPI", Math.max( java.awt.Toolkit.getDefaultToolkit( ).getScreenResolution( ), 96 ) + "" );
                updateConfigFile( );
            }
            float menuScaling = Float.parseFloat( properties.getProperty( "MenuDPI", "96" ) ) / 96;
            java.awt.Font font = theme.getUserTextFont( ).deriveFont( theme.getUserTextFont( ).getSize( ) * menuScaling );
            theme.setFont( font );
            
            NimRODLookAndFeel.setCurrentTheme( theme );
            JFrame.setDefaultLookAndFeelDecorated( true );
            JDialog.setDefaultLookAndFeelDecorated( true );
            // all done
            lookAndFeel.initialize( );
            UIManager.setLookAndFeel( lookAndFeel );
        }
        catch( Exception ex )
        {
            try
            {
                UIManager.setLookAndFeel( UIManager.getCrossPlatformLookAndFeelClassName( ) );
            }
            catch( Exception ex1 )
            {
                log.log( Level.SEVERE, "Look & Feel unsupported.", ex1 );
                exit( );
            }
        }
        
        // Get the image sets to use
        if( !Boolean.parseBoolean( properties.getProperty( "AlwaysShowShimejiChooser", "false" ) ) )
        {
            for( String set : properties.getProperty( "ActiveShimeji", "" ).split( "/" ) )
                if( !set.trim( ).isEmpty( ) )
                    imageSets.add( set.trim( ) );
        }
        do
        {
            if( imageSets.isEmpty( ) )
            {
                imageSets = new ImageSetChooser( frame, true ).display( );
                if( imageSets == null )
                    exit( );
            }

            // Load shimejis
            for( int index = 0; index < imageSets.size( ); index++ )
            {
                if( loadConfiguration( imageSets.get( index ) ) == false )
                {
                    // failed validation
                    configurations.remove( imageSets.get( index ) );
                    imageSets.remove( imageSets.get( index ) );
                    index--;
                }
            }
        }
        while( imageSets.isEmpty( ) );

        // Create the tray icon
        createTrayIcon( );
        
        // Create the first mascot
        for( String imageSet : imageSets )
        {
            String informationAlreadySeen = properties.getProperty( "InformationDismissed", "" );
            if( configurations.get( imageSet ).containsInformationKey( "SplashImage" ) &&
                ( Boolean.parseBoolean( properties.getProperty( "AlwaysShowInformationScreen", "false" ) ) ||
                  !informationAlreadySeen.contains( imageSet ) ) )
            {
                InformationWindow info = new InformationWindow( );
                info.init( imageSet, configurations.get( imageSet ) );
                info.display( );
                setMascotInformationDismissed( imageSet );
                updateConfigFile( );
            }
            createMascot( imageSet );
        }

        getManager( ).start( );
    }

    private boolean loadConfiguration( final String imageSet )
    {
        try
        {
            // try to load in the correct xml files
            Path filePath = Paths.get( ".", "conf" );
            Path actionsPath = filePath.resolve( "actions.xml" );
            if( filePath.resolve( "\u52D5\u4F5C.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u52D5\u4F5C.xml" );
            
            filePath = Paths.get( ".", "conf", imageSet );
            if( filePath.resolve( "actions.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "actions.xml" );
            else if( filePath.resolve( "\u52D5\u4F5C.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u52D5\u4F5C.xml" );
            else if( filePath.resolve( "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml" );
            else if( filePath.resolve( "\u00A6-\u00BA@.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u00A6-\u00BA@.xml" );
            else if( filePath.resolve( "\u00F4\u00AB\u00EC\u00FD.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u00F4\u00AB\u00EC\u00FD.xml" );
            else if( filePath.resolve( "one.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "one.xml" );
            else if( filePath.resolve( "1.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "1.xml" );
            
            filePath = Paths.get( ".", "img", imageSet, "conf" );
            if( filePath.resolve( "actions.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "actions.xml" );
            else if( filePath.resolve( "\u52D5\u4F5C.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u52D5\u4F5C.xml" );
            else if( filePath.resolve( "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u00D5\u00EF\u00F2\u00F5\u00A2\u00A3.xml" );
            else if( filePath.resolve( "\u00A6-\u00BA@.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u00A6-\u00BA@.xml" );
            else if( filePath.resolve( "\u00F4\u00AB\u00EC\u00FD.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "\u00F4\u00AB\u00EC\u00FD.xml" );
            else if( filePath.resolve( "one.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "one.xml" );
            else if( filePath.resolve( "1.xml" ).toFile( ).exists( ) )
                actionsPath = filePath.resolve( "1.xml" );

            log.log( Level.INFO, imageSet + " Read Action File ({0})", actionsPath );

            final Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream( actionsPath.toFile( ) ) );
            
            Configuration configuration = new Configuration( );

            configuration.load( new Entry( actions.getDocumentElement( ) ), imageSet );

            filePath = Paths.get( ".", "conf" );
            Path behaviorsPath = filePath.resolve( "behaviors.xml" );
            if( filePath.resolve( "\u884C\u52D5.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u884C\u52D5.xml" );
            
            filePath = Paths.get( ".", "conf", imageSet );
            if( filePath.resolve( "behaviors.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "behaviors.xml" );
            else if( filePath.resolve( "behavior.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "behavior.xml" );
            else if( filePath.resolve( "\u884C\u52D5.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u884C\u52D5.xml" );
            else if( filePath.resolve( "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml" );
            else if( filePath.resolve( "\u00AA\u00B5\u00A6-.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u00AA\u00B5\u00A6-.xml" );
            else if( filePath.resolve( "\u00ECs\u00F4\u00AB.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u00ECs\u00F4\u00AB.xml" );
            else if( filePath.resolve( "two.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "two.xml" );
            else if( filePath.resolve( "2.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "2.xml" );
            
            filePath = Paths.get( ".", "img", imageSet, "conf" );
            if( filePath.resolve( "behaviors.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "behaviors.xml" );
            else if( filePath.resolve( "behavior.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "behavior.xml" );
            else if( filePath.resolve( "\u884C\u52D5.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u884C\u52D5.xml" );
            else if( filePath.resolve( "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u00DE\u00ED\u00EE\u00D5\u00EF\u00F2.xml" );
            else if( filePath.resolve( "\u00AA\u00B5\u00A6-.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u00AA\u00B5\u00A6-.xml" );
            else if( filePath.resolve( "\u00ECs\u00F4\u00AB.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "\u00ECs\u00F4\u00AB.xml" );
            else if( filePath.resolve( "two.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "two.xml" );
            else if( filePath.resolve( "2.xml" ).toFile( ).exists( ) )
                behaviorsPath = filePath.resolve( "2.xml" );

            log.log( Level.INFO, imageSet + " Read Behavior File ({0})", behaviorsPath );

            final Document behaviors = DocumentBuilderFactory.newInstance( ).newDocumentBuilder( ).parse(new FileInputStream( behaviorsPath.toFile( ) ) );

            configuration.load( new Entry( behaviors.getDocumentElement( ) ), imageSet );

            filePath = Paths.get( ".", "conf" );
            Path infoPath = filePath.resolve( "info.xml" );
            
            filePath = Paths.get( ".", "conf", imageSet );
            if( filePath.resolve( "info.xml" ).toFile( ).exists( ) )
                infoPath = filePath.resolve( "info.xml" );
            
            filePath = Paths.get( ".", "img", imageSet, "conf" );
            if( filePath.resolve( "info.xml" ).toFile( ).exists( ) )
                infoPath = filePath.resolve( "info.xml" );

            if( infoPath.toFile( ).exists( ) )
            {
                log.log( Level.INFO, imageSet + " Read Information File ({0})", infoPath );

                final Document information = DocumentBuilderFactory.newInstance( ).newDocumentBuilder( ).parse(new FileInputStream( infoPath.toFile( ) ) );

                configuration.load( new Entry( information.getDocumentElement( ) ), imageSet );
            }
            
            configuration.validate( );

            configurations.put( imageSet, configuration );
            
            ArrayList<String> childMascots = new ArrayList<String>( );
            
            // born mascot bit goes here...
            for( final Entry list : new Entry( actions.getDocumentElement( ) ).selectChildren( "ActionList" ) )
            {
                for( final Entry node : list.selectChildren( "Action" ) )
                {
                    if( node.getAttributes( ).containsKey( "BornMascot" ) )
                    {
                        String set = node.getAttribute( "BornMascot" );
                        if( !childMascots.contains( set ) )
                            childMascots.add( set );
                        if( !configurations.containsKey( set ) )
                            loadConfiguration( set );
                    }
                    if( node.getAttributes( ).containsKey( "TransformMascot" ) )
                    {
                        String set = node.getAttribute( "TransformMascot" );
                        if( !childMascots.contains( set ) )
                            childMascots.add( set );
                        if( !configurations.containsKey( set ) )
                            loadConfiguration( set );
                    }
                }
            }
            
            childImageSets.put( imageSet, childMascots );

            return true;
        }
        catch( final Exception e )
        {
            log.log( Level.SEVERE, "Failed to load configuration files", e );
            Main.showError( languageBundle.getString( "FailedLoadConfigErrorMessage" ), e );
        }
        
        return false;
    }

    /**
     * Create a tray icon.
     *
     * @ Throws AWTException
     * @ Throws IOException
     */
    private void createTrayIcon( )
    {
        log.log( Level.INFO, "create a tray icon" );
        
        // get the tray icon image
        BufferedImage image = null;
        try
        {
            image = ImageIO.read( Paths.get( ".", "img", "icon.png" ).toFile( ) );
        }
        catch( final Exception e )
        {
            log.log( Level.SEVERE, "Failed to create tray icon", e );
            Main.showError( languageBundle.getString( "FailedDisplaySystemTrayErrorMessage" ), e );
        }
        finally
        {
            if( image == null )
                image = new BufferedImage( 16, 16, BufferedImage.TYPE_INT_RGB );
        }

        try
        {
            // Create the tray icon
            String caption = properties.getProperty( "ShimejiEENameOverride", "" ).trim( );
            if( caption.isEmpty( ) )
                caption = languageBundle.getString( "ShimejiEE" );
            final TrayIcon icon = new TrayIcon( image, caption );
            
            // attach menu
            icon.addMouseListener( new MouseListener( )
            {
                @Override
                public void mouseClicked( MouseEvent event )
                {
                }

                @Override
                public void mousePressed( MouseEvent e )
                {
                }

                @Override
                public void mouseReleased( MouseEvent event )
                {
                    if( event.isPopupTrigger( ) )
                    {
                        // close the form if it's open
                        if( form != null )
                            form.dispose( );
                        
                        // create the form and border
                        form = new JDialog( frame, false );
                        final JPanel panel = new JPanel( );
                        panel.setBorder( new EmptyBorder( 5, 5, 5, 5 ) );
                        form.add( panel );
        
                        // buttons and action handling
                        JButton btnCallShimeji = new JButton( languageBundle.getString( "CallShimeji" ) );
                        btnCallShimeji.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                createMascot( );
                                form.dispose( );
                            }
                        } );
                        
                        JButton btnFollowCursor = new JButton( languageBundle.getString( "FollowCursor" ) );
                        btnFollowCursor.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                getManager( ).setBehaviorAll( BEHAVIOR_GATHER );
                                form.dispose( );
                            }
                        } );
                        
                        JButton btnReduceToOne = new JButton( languageBundle.getString( "ReduceToOne" ) );
                        btnReduceToOne.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                getManager( ).remainOne( );
                                form.dispose( );
                            }
                        } );
                        
                        JButton btnRestoreWindows = new JButton( languageBundle.getString( "RestoreWindows" ) );
                        btnRestoreWindows.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                NativeFactory.getInstance( ).getEnvironment( ).restoreIE( );
                                form.dispose( );
                            }
                        } );
                        
                        final JButton btnAllowedBehaviours = new JButton( languageBundle.getString( "AllowedBehaviours" ) );
                        btnAllowedBehaviours.addMouseListener( new MouseListener( )
                        {
                            @Override
                            public void mouseClicked( MouseEvent e )
                            {
                            }

                            @Override
                            public void mousePressed( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseReleased( MouseEvent e )
                            {
                                btnAllowedBehaviours.setEnabled( true );
                            }

                            @Override
                            public void mouseEntered( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseExited( MouseEvent e )
                            {
                            }
                        } );
                        btnAllowedBehaviours.addActionListener( new ActionListener( )
                        {
                            @Override
                            public void actionPerformed( final ActionEvent event )
                            {
                                // "Disable Breeding" menu item
                                final JCheckBoxMenuItem breedingMenu = new JCheckBoxMenuItem( languageBundle.getString( "BreedingCloning" ), Boolean.parseBoolean( properties.getProperty( "Breeding", "true" ) ) );
                                breedingMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        breedingMenu.setState( toggleBooleanSetting( "Breeding", true ) );
                                        updateConfigFile( );
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );
                                
                                // "Disable Breeding Transient" menu item
                                final JCheckBoxMenuItem transientMenu = new JCheckBoxMenuItem( languageBundle.getString( "BreedingTransient" ), Boolean.parseBoolean( properties.getProperty( "Transients", "true" ) ) );
                                transientMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        transientMenu.setState( toggleBooleanSetting( "Transients", true ) );
                                        updateConfigFile( );
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );
                                
                                // "Disable Transformations" menu item
                                final JCheckBoxMenuItem transformationMenu = new JCheckBoxMenuItem( languageBundle.getString( "Transformation" ), Boolean.parseBoolean( properties.getProperty( "Transformation", "true" ) ) );
                                transformationMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        transformationMenu.setState( toggleBooleanSetting( "Transformation", true ) );
                                        updateConfigFile( );
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );

                                // "Throwing Windows" menu item
                                final JCheckBoxMenuItem throwingMenu = new JCheckBoxMenuItem( languageBundle.getString( "ThrowingWindows" ), Boolean.parseBoolean( properties.getProperty( "Throwing", "true" ) ) );
                                throwingMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        throwingMenu.setState( toggleBooleanSetting( "Throwing", true ) );
                                        updateConfigFile( );
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );

                                // "Mute Sounds" menu item
                                final JCheckBoxMenuItem soundsMenu = new JCheckBoxMenuItem( languageBundle.getString( "SoundEffects" ), Boolean.parseBoolean( properties.getProperty( "Sounds", "true" ) ) );
                                soundsMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        boolean result = toggleBooleanSetting( "Sounds", true );
                                        soundsMenu.setState( result );
                                        Sounds.setMuted( !result );
                                        updateConfigFile( );
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );

                                // "Multiscreen" menu item
                                final JCheckBoxMenuItem multiscreenMenu = new JCheckBoxMenuItem( languageBundle.getString( "Multiscreen" ), Boolean.parseBoolean( properties.getProperty( "Multiscreen", "true" ) ) );
                                multiscreenMenu.addItemListener( new ItemListener( )
                                {
                                    public void itemStateChanged( final ItemEvent e )
                                    {
                                        multiscreenMenu.setState( toggleBooleanSetting( "Multiscreen", true ) );
                                        updateConfigFile( );
                                        btnAllowedBehaviours.setEnabled( true );
                                    }
                                } );
                                
                                JPopupMenu behaviourPopup = new JPopupMenu( );
                                behaviourPopup.add( breedingMenu );
                                behaviourPopup.add( transientMenu );
                                behaviourPopup.add( transformationMenu );
                                behaviourPopup.add( throwingMenu );
                                behaviourPopup.add( soundsMenu );
                                behaviourPopup.add( multiscreenMenu );
                                behaviourPopup.addPopupMenuListener( new PopupMenuListener( )
                                {
                                    @Override
                                    public void popupMenuWillBecomeVisible( PopupMenuEvent e )
                                    {
                                    }

                                    @Override
                                    public void popupMenuWillBecomeInvisible( PopupMenuEvent e )
                                    {
                                        if( panel.getMousePosition( ) != null )
                                        {
                                            btnAllowedBehaviours.setEnabled( !( panel.getMousePosition( ).x > btnAllowedBehaviours.getX( ) && 
                                                panel.getMousePosition( ).x < btnAllowedBehaviours.getX( ) + btnAllowedBehaviours.getWidth( ) &&
                                                panel.getMousePosition( ).y > btnAllowedBehaviours.getY( ) && 
                                                panel.getMousePosition( ).y < btnAllowedBehaviours.getY( ) + btnAllowedBehaviours.getHeight( ) ) );
                                        }
                                        else
                                        {
                                            btnAllowedBehaviours.setEnabled( true );
                                        }
                                    }

                                    @Override
                                    public void popupMenuCanceled( PopupMenuEvent e )
                                    {
                                    }
                                } );
                                behaviourPopup.show( btnAllowedBehaviours, 0, btnAllowedBehaviours.getHeight( ) );
                                btnAllowedBehaviours.requestFocusInWindow( );
                            }
                        } );
                        
                        final JButton btnChooseShimeji = new JButton( languageBundle.getString( "ChooseShimeji" ) );
                        btnChooseShimeji.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                form.dispose( );
                                if( !getManager( ).isPaused( ) )
                                    getManager( ).togglePauseAll( ); // needed to stop the guys from potentially throwing away settings
                                
                                ImageSetChooser chooser = new ImageSetChooser( frame, true );
                                setActiveImageSets( chooser.display( ) );
                                
                                if( getManager( ).isPaused( ) )
                                    getManager( ).togglePauseAll( );
                            }
                        } );
                        
                        final JButton btnSettings = new JButton( languageBundle.getString( "Settings" ) );
                        btnSettings.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent event )
                            {
                                form.dispose( );
                                if( !getManager( ).isPaused( ) )
                                    getManager( ).togglePauseAll( ); // needed to stop the guys from potentially throwing away settings
                                
                                SettingsWindow dialog = new SettingsWindow( frame, true );
                                dialog.setIconImage( icon.getImage( ) );
                                dialog.init( );
                                dialog.display( );
                                
                                if( dialog.getEnvironmentReloadRequired( ) )
                                {
                                    NativeFactory.getInstance( ).getEnvironment( ).dispose( );
                                    NativeFactory.resetInstance( );
                                }
                                if( dialog.getEnvironmentReloadRequired( ) || dialog.getImageReloadRequired( ) )
                                {
                                    // need to reload the shimeji as the images have rescaled
                                    boolean isExit = getManager( ).isExitOnLastRemoved( );
                                    getManager( ).setExitOnLastRemoved( false );
                                    getManager( ).disposeAll( );

                                    // Wipe all loaded data
                                    ImagePairs.clear( );
                                    configurations.clear( );

                                    // Load settings
                                    for( String imageSet : imageSets )
                                    {
                                        loadConfiguration( imageSet );
                                    }

                                    // Create the first mascot
                                    for( String imageSet : imageSets )
                                    {
                                        createMascot( imageSet );
                                    }

                                    Main.this.getManager( ).setExitOnLastRemoved( isExit );
                                }
                                else
                                {
                                    if( getManager( ).isPaused( ) )
                                        getManager( ).togglePauseAll( );
                                }
                                if( dialog.getInteractiveWindowReloadRequired( ) )
                                    NativeFactory.getInstance( ).getEnvironment( ).refreshCache( );
                            }
                        } );
                        
                        final JButton btnLanguage = new JButton( languageBundle.getString( "Language" ) );
                        btnLanguage.addMouseListener( new MouseListener( )
                        {
                            @Override
                            public void mouseClicked( MouseEvent e )
                            {
                            }

                            @Override
                            public void mousePressed( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseReleased( MouseEvent e )
                            {
                                btnLanguage.setEnabled( true );
                            }

                            @Override
                            public void mouseEntered( MouseEvent e )
                            {
                            }

                            @Override
                            public void mouseExited( MouseEvent e )
                            {
                            }
                        } );
                        btnLanguage.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent e )
                            {
                                // English menu item
                                final JMenuItem englishMenu = new JMenuItem( "English" );
                                englishMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "en-GB" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Arabic menu item
                                final JMenuItem arabicMenu = new JMenuItem( "\u0639\u0631\u0628\u064A" );
                                arabicMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "ar-SA" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Catalan menu item
                                final JMenuItem catalanMenu = new JMenuItem( "Catal\u00E0" );
                                catalanMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "ca-ES" );
                                        updateConfigFile( );
                                    }
                                } );

                                // German menu item
                                final JMenuItem germanMenu = new JMenuItem( "Deutsch" );
                                germanMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "de-DE" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Spanish menu item
                                final JMenuItem spanishMenu = new JMenuItem( "Espa\u00F1ol" );
                                spanishMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "es-ES" );
                                        updateConfigFile( );
                                    }
                                } );

                                // French menu item
                                final JMenuItem frenchMenu = new JMenuItem( "Fran\u00E7ais" );
                                frenchMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "fr-FR" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Croatian menu item
                                final JMenuItem croatianMenu = new JMenuItem( "Hrvatski" );
                                croatianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "hr-HR" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Italian menu item
                                final JMenuItem italianMenu = new JMenuItem( "Italiano" );
                                italianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "it-IT" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Dutch menu item
                                final JMenuItem dutchMenu = new JMenuItem( "Nederlands" );
                                dutchMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "nl-NL" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Polish menu item
                                final JMenuItem polishMenu = new JMenuItem( "Polski" );
                                polishMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "pl-PL" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Brazilian Portuguese menu item
                                final JMenuItem brazilianPortugueseMenu = new JMenuItem( "Portugu\u00eas Brasileiro" );
                                brazilianPortugueseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "pt-BR" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Portuguese menu item
                                final JMenuItem portugueseMenu = new JMenuItem( "Portugu\u00eas" );
                                portugueseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "pt-PT" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Russian menu item
                                final JMenuItem russianMenu = new JMenuItem( "\u0440\u0443\u0301\u0441\u0441\u043a\u0438\u0439 \u044f\u0437\u044b\u0301\u043a" );
                                russianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "ru-RU" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Romanian menu item
                                final JMenuItem romanianMenu = new JMenuItem( "Rom\u00e2n\u0103" );
                                romanianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "ro-RO" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Srpski menu item
                                final JMenuItem serbianMenu = new JMenuItem( "Srpski" );
                                serbianMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "sr-RS" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Finnish menu item
                                final JMenuItem finnishMenu = new JMenuItem( "Suomi" );
                                finnishMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "fi-FI" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Vietnamese menu item
                                final JMenuItem vietnameseMenu = new JMenuItem( "ti\u1ebfng Vi\u1ec7t" );
                                vietnameseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "vi-VN" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Chinese menu item
                                final JMenuItem chineseMenu = new JMenuItem( "\u7b80\u4f53\u4e2d\u6587" );
                                chineseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "zh-CN" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Chinese (Traditional) menu item
                                final JMenuItem chineseTraditionalMenu = new JMenuItem( "\u7E41\u9AD4\u4E2D\u6587" );
                                chineseTraditionalMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "zh-TW" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Korean menu item
                                final JMenuItem koreanMenu = new JMenuItem( "\ud55c\uad6d\uc5b4" );
                                koreanMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "ko-KR" );
                                        updateConfigFile( );
                                    }
                                } );

                                // Japanese menu item
                                final JMenuItem japaneseMenu = new JMenuItem( "\u65E5\u672C\u8A9E" );
                                japaneseMenu.addActionListener( new ActionListener( )
                                {
                                    public void actionPerformed( final ActionEvent e )
                                    {
                                        form.dispose( );
                                        updateLanguage( "ja-JP" );
                                        updateConfigFile( );
                                    }
                                } );
                                
                                JPopupMenu languagePopup = new JPopupMenu( );
                                languagePopup.add( englishMenu );
                                languagePopup.addSeparator( );
                                languagePopup.add( arabicMenu );
                                languagePopup.add( catalanMenu );
                                languagePopup.add( germanMenu );
                                languagePopup.add( spanishMenu );
                                languagePopup.add( frenchMenu );
                                languagePopup.add( croatianMenu );
                                languagePopup.add( italianMenu );
                                languagePopup.add( dutchMenu );
                                languagePopup.add( polishMenu );
                                languagePopup.add( portugueseMenu );
                                languagePopup.add( brazilianPortugueseMenu );
                                languagePopup.add( russianMenu );
                                languagePopup.add( romanianMenu );
                                languagePopup.add( serbianMenu );
                                languagePopup.add( finnishMenu );
                                languagePopup.add( vietnameseMenu );
                                languagePopup.add( chineseMenu );
                                languagePopup.add( chineseTraditionalMenu );
                                languagePopup.add( koreanMenu );
                                languagePopup.add( japaneseMenu );
                                languagePopup.addPopupMenuListener( new PopupMenuListener( )
                                {
                                    @Override
                                    public void popupMenuWillBecomeVisible( PopupMenuEvent e )
                                    {
                                    }

                                    @Override
                                    public void popupMenuWillBecomeInvisible( PopupMenuEvent e )
                                    {
                                        if( panel.getMousePosition( ) != null )
                                        {
                                            btnLanguage.setEnabled( !( panel.getMousePosition( ).x > btnLanguage.getX( ) && 
                                                panel.getMousePosition( ).x < btnLanguage.getX( ) + btnLanguage.getWidth( ) &&
                                                panel.getMousePosition( ).y > btnLanguage.getY( ) && 
                                                panel.getMousePosition( ).y < btnLanguage.getY( ) + btnLanguage.getHeight( ) ) );
                                        }
                                        else
                                        {
                                            btnLanguage.setEnabled( true );
                                        }
                                    }

                                    @Override
                                    public void popupMenuCanceled( PopupMenuEvent e )
                                    {
                                    }
                                } );
                                languagePopup.show( btnLanguage, 0, btnLanguage.getHeight( ) );
                                btnLanguage.requestFocusInWindow( );
                            }
                        } );
                        
                        JButton btnPauseAll = new JButton( getManager( ).isPaused( ) ? languageBundle.getString( "ResumeAnimations" ) : languageBundle.getString( "PauseAnimations" ) );
                        btnPauseAll.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent e )
                            {
                                form.dispose( );
                                getManager( ).togglePauseAll( );
                            }
                        } );
                        
                        JButton btnDismissAll = new JButton( languageBundle.getString( "DismissAll" ) );
                        btnDismissAll.addActionListener( new ActionListener( )
                        {
                            public void actionPerformed( final ActionEvent e )
                            {
                                exit( );
                            }
                        } );

                        // layout
                        float scaling = Float.parseFloat( properties.getProperty( "MenuDPI", "96" ) ) / 96;
                        panel.setLayout( new java.awt.GridBagLayout( ) );
                        GridBagConstraints gridBag = new GridBagConstraints( );
                        gridBag.fill = GridBagConstraints.HORIZONTAL;
                        gridBag.gridx = 0;
                        gridBag.gridy = 0;
                        panel.add( btnCallShimeji, gridBag );
                        gridBag.insets = new Insets( (int)( 5 * scaling ), 0, 0, 0 );
                        gridBag.gridy++;
                        panel.add( btnFollowCursor, gridBag );
                        gridBag.gridy++;
                        panel.add( btnReduceToOne, gridBag );
                        gridBag.gridy++;
                        panel.add( btnRestoreWindows, gridBag );
                        gridBag.gridy++;
                        panel.add( new JSeparator( ), gridBag );
                        gridBag.gridy++;
                        panel.add( btnAllowedBehaviours, gridBag );
                        gridBag.gridy++;
                        panel.add( btnChooseShimeji, gridBag );
                        gridBag.gridy++;
                        panel.add( btnSettings, gridBag );
                        gridBag.gridy++;
                        panel.add( btnLanguage, gridBag );
                        gridBag.gridy++;
                        panel.add( new JSeparator( ), gridBag );
                        gridBag.gridy++;
                        panel.add( btnPauseAll, gridBag );
                        gridBag.gridy++;
                        panel.add( btnDismissAll, gridBag );

                        form.setIconImage( icon.getImage( ) );
                        form.setTitle( icon.getToolTip( ) );
                        form.setDefaultCloseOperation( javax.swing.WindowConstants.DISPOSE_ON_CLOSE );
                        form.setAlwaysOnTop( true );
                        
                        // set the form dimensions
                        java.awt.FontMetrics metrics = btnCallShimeji.getFontMetrics( btnCallShimeji.getFont( ) );
                        int width = metrics.stringWidth( btnCallShimeji.getText( ) );
                        width = Math.max( metrics.stringWidth( btnFollowCursor.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnReduceToOne.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnRestoreWindows.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnAllowedBehaviours.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnChooseShimeji.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnSettings.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnLanguage.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnPauseAll.getText( ) ), width );
                        width = Math.max( metrics.stringWidth( btnDismissAll.getText( ) ), width );
                        panel.setPreferredSize( new Dimension( width + 64, 
                                (int)( 24 * scaling ) + // 12 padding on top and bottom
                                (int)( 75 * scaling ) + // 13 insets of 5 height normally
                                10 * metrics.getHeight( ) + // 10 button faces
                                84 ) );
                        form.pack( );
                        
                        // setting location of the form
                        form.setLocation( event.getPoint( ).x - form.getWidth( ), event.getPoint( ).y - form.getHeight( ) );
                        
                        // make sure that it is on the screen if people are using exotic taskbar locations
                        Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment( ).getMaximumWindowBounds( );
                        if( form.getX( ) < screen.getX( ) )
                        {
                            form.setLocation( event.getPoint( ).x, form.getY( ) );
                        }
                        if( form.getY( ) < screen.getY( ) )
                        {
                            form.setLocation( form.getX( ), event.getPoint( ).y );
                        }
                        form.setVisible( true );
                        form.setMinimumSize( form.getSize( ) );
                    }
                    else if( event.getButton( ) == MouseEvent.BUTTON1 )
                    {
                        createMascot( );
                    }
                    else if( event.getButton( ) == MouseEvent.BUTTON2 && event.getClickCount( ) == 2 )
                    {
                        if( getManager( ).isExitOnLastRemoved( ) )
                        {
                            getManager( ).setExitOnLastRemoved( false );
                            getManager( ).disposeAll( );
                        }
                        else
                        {
                            for( String imageSet : imageSets )
                            {
                                createMascot( imageSet );
                            }
                            getManager( ).setExitOnLastRemoved( true );
                        }
                    }
                }

                @Override
                public void mouseEntered( MouseEvent e )
                {
                }

                @Override
                public void mouseExited( MouseEvent e )
                {
                }
            } );

            // Show tray icon
            SystemTray.getSystemTray( ).add( icon );
        }
        catch( final AWTException e )
        {
            log.log( Level.SEVERE, "Failed to create tray icon", e );
            Main.showError( languageBundle.getString( "FailedDisplaySystemTrayErrorMessage" ) + "\n" + languageBundle.getString( "SeeLogForDetails" ) );
            exit( );
        }
    }

    // Randomly creates a mascot
    public void createMascot( )
    {
        int length = imageSets.size( );
        int random = ( int ) ( length * Math.random( ) );
        createMascot( imageSets.get( random ) );
    }

    /**
     * Create a mascot
     */
    public void createMascot( String imageSet )
    {
        log.log( Level.INFO, "create a mascot" );

        // Create one mascot
        final Mascot mascot = new Mascot( imageSet );

        // Create it outside the bounds of the screen
        mascot.setAnchor( new Point( -4000, -4000 ) );

        // Randomize the initial orientation
        mascot.setLookRight( Math.random( ) < 0.5 );

        try
        {
            mascot.setBehavior( getConfiguration( imageSet ).buildNextBehavior( null, mascot ) );
            this.getManager().add( mascot );
        }
        catch( final BehaviorInstantiationException e )
        {
            log.log( Level.SEVERE, "Failed to initialize the first action", e );
            Main.showError( languageBundle.getString( "FailedInitialiseFirstActionErrorMessage" ), e );
            mascot.dispose( );
        }
        catch( final CantBeAliveException e )
        {
            log.log( Level.SEVERE, "Fatal Error", e );
            Main.showError( languageBundle.getString( "FailedInitialiseFirstActionErrorMessage" ), e );
            mascot.dispose( );
        }
        catch( Exception e )
        {
            log.log( Level.SEVERE, imageSet + " fatal error, can not be started.", e );
            Main.showError( languageBundle.getString( "CouldNotCreateShimejiErrorMessage" ) + " " + imageSet, e );
            mascot.dispose( );
        }
    }
    
    private void refreshLanguage( )
    {
        ResourceBundle.Control utf8Control = new Utf8ResourceBundleControl( false );
        languageBundle = ResourceBundle.getBundle( "language", Locale.forLanguageTag( properties.getProperty( "Language", "en-GB" ) ), utf8Control );

        boolean isExit = getManager( ).isExitOnLastRemoved( );
        getManager( ).setExitOnLastRemoved( false );
        getManager( ).disposeAll( );

        // Load settings
        for( String imageSet : imageSets )
        {
            loadConfiguration( imageSet );
        }

        // Create the first mascot
        for( String imageSet : imageSets )
        {
            createMascot( imageSet );
        }

        getManager( ).setExitOnLastRemoved( isExit );
    }
    
    private void updateLanguage( String language )
    {
        if( !properties.getProperty( "Language", "en-GB" ).equals( language ) )
        {
            properties.setProperty( "Language", language );
            refreshLanguage( );
        }        
    }
    
    private boolean toggleBooleanSetting( String propertyName, boolean defaultValue )
    {
        if( Boolean.parseBoolean( properties.getProperty( propertyName, defaultValue + "" ) ) )
        {
            properties.setProperty( propertyName, "false" );
            return false;
        }
        else
        {
            properties.setProperty( propertyName, "true" );
            return true;
        }
    }
    
    private void setMascotInformationDismissed( final String imageSet )
    {
        ArrayList<String> list = new ArrayList<String>( );
        String[ ] data = properties.getProperty( "InformationDismissed", "" ).split( "/" );
        
        if( data.length > 0 && !data[ 0 ].equals( "" ) )
            list.addAll( Arrays.asList( data ) );
        if( !list.contains( imageSet ) )
            list.add( imageSet );
        
        properties.setProperty( "InformationDismissed", list.toString( ).replace( "[", "" ).replace( "]", "" ).replace( ", ", "/" ) );
    }
    
    public void setMascotBehaviorEnabled( final String name, final Mascot mascot, boolean enabled )
    {
        ArrayList<String> list = new ArrayList<String>( );
        String[ ] data = properties.getProperty( "DisabledBehaviours." + mascot.getImageSet( ), "" ).split( "/" );
        
        if( data.length > 0 && !data[ 0 ].equals( "" ) )
            list.addAll( Arrays.asList( data ) );
        
        if( list.contains( name ) && enabled )
            list.remove( name );
        else if( !list.contains( name ) && !enabled )
            list.add( name );
        
        if( list.size( ) > 0 )
            properties.setProperty( "DisabledBehaviours." + mascot.getImageSet( ), list.toString( ).replace( "[", "" ).replace( "]", "" ).replace( ", ", "/" ) );
        else
            properties.remove( "DisabledBehaviours." + mascot.getImageSet( ) );
        
        updateConfigFile( );
    }
    
    private void updateConfigFile( )
    {
        try
        {
            FileOutputStream output = new FileOutputStream( Paths.get( ".", "conf", "settings.properties" ).toFile( ) );
            try
            {
                properties.store( output, "Shimeji-ee Configuration Options" );
            }
            finally
            {
                output.close( );
            }
        }
        catch( Exception unimportant )
        {
        }
    }
    
    /** 
     * Replaces the current set of active imageSets without modifying
     * valid imageSets that are already active. Does nothing if newImageSets 
     * are null
     *
     * @param newImageSets All the imageSets that should now be active
     * @author snek, with some tweaks by Kilkakon
     * */
    private void setActiveImageSets( ArrayList<String> newImageSets )
    {
        if( newImageSets == null )
            return;

        // I don't think there would be enough imageSets chosen at any given
        // time for it to be worth using HashSet but i might be wrong
        ArrayList<String> toRemove = new ArrayList<String>( imageSets );
        toRemove.removeAll( newImageSets );

        ArrayList<String> toAdd = new ArrayList<String>( );
        ArrayList<String> toRetain = new ArrayList<String>( );
        for( String set : newImageSets )
        {
            if( !imageSets.contains( set ) )
                toAdd.add( set );
            if( !toRetain.contains( set ) )
                toRetain.add( set );
            populateArrayListWithChildSets( set, toRetain );
        }
        
        boolean isExit = Main.this.getManager( ).isExitOnLastRemoved( );
        Main.this.getManager( ).setExitOnLastRemoved( false );

        for( String r : toRemove )
            removeLoadedImageSet( r, toRetain );
        
        for( String a : toAdd )
            addImageSet( a );
        
        Main.this.getManager( ).setExitOnLastRemoved( isExit );
    }

    private void populateArrayListWithChildSets( String imageSet, ArrayList<String> childList )
    {
        if( childImageSets.containsKey( imageSet ) )
        {
            for( String set : childImageSets.get( imageSet ) )
            {
                if( !childList.contains( set ) )
                {
                    populateArrayListWithChildSets( set, childList );
                    childList.add( set );
                }
            }
        }
    }

    private void removeLoadedImageSet( String imageSet, ArrayList<String> setsToIgnore )
    {
        if( childImageSets.containsKey( imageSet ) )
        {
            for( String set : childImageSets.get( imageSet ) )
            {
                if( !setsToIgnore.contains( set ) )
                {
                    setsToIgnore.add( set );
                    imageSets.remove( imageSet );
                    getManager( ).remainNone( imageSet );
                    configurations.remove( imageSet );
                    ImagePairs.removeAll( imageSet );
                    removeLoadedImageSet( set, setsToIgnore );
                }
            }
        }
        
        if( !setsToIgnore.contains( imageSet ) )
        {
            imageSets.remove( imageSet );
            getManager( ).remainNone( imageSet );
            configurations.remove( imageSet );
            ImagePairs.removeAll( imageSet );
        }
    }
    
    private void addImageSet( String imageSet )
    {
        if( configurations.containsKey( imageSet ) )
        {
            imageSets.add( imageSet );
            createMascot( imageSet );
        }
        else
        {
            if( loadConfiguration( imageSet ) )
            {
                imageSets.add( imageSet );
                String informationAlreadySeen = properties.getProperty( "InformationDismissed", "" );
                if( configurations.get( imageSet ).containsInformationKey( "SplashImage" ) &&
                    ( Boolean.parseBoolean( properties.getProperty( "AlwaysShowInformationScreen", "false" ) ) ||
                      !informationAlreadySeen.contains( imageSet ) ) )
                {
                    InformationWindow info = new InformationWindow( );
                    info.init( imageSet, configurations.get( imageSet ) );
                    info.display( );
                    setMascotInformationDismissed( imageSet );
                    updateConfigFile( );
                }
                createMascot( imageSet );
            }
            else
            {
                // conf failed
                configurations.remove( imageSet ); // maybe move this to the loadConfig catch
            }
        }
    }

    public Configuration getConfiguration( String imageSet )
    {
        return configurations.get( imageSet );
    }

    private Manager getManager( )
    {
        return this.manager;
    }
        
    public Platform getPlatform( )
    {
        return platform;
    }

    public Properties getProperties( )
    {
        return properties;
    }

    public ResourceBundle getLanguageBundle( )
    {
        return languageBundle;
    }

    public void exit( )
    {
        this.getManager( ).disposeAll( );
        this.getManager( ).stop( );
        System.exit( 0 );
    }
}
