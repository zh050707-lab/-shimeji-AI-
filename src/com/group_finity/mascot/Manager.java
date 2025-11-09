package com.group_finity.mascot;

import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;       // 导入弹出菜单类
import java.awt.event.ActionEvent; // 导入动作事件类
import java.awt.event.ActionListener; // 导入动作监听器接口
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.ai.AiChatService; // 导入我们的新接口
import com.group_finity.mascot.ai.DeepseekChatService; // 导入 Deepseek 实现
import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.memory.MemoryManager; // 导入内存管理器



/**
 * 
 * Maintains a list of mascot, the object to time.
 * 
 * Original Author: Yuki Yamada of Group Finity (http://www.group-finity.com/Shimeji/)
 * Currently developed by Shimeji-ee Group.
 */
public class Manager {

	private static final Logger log = Logger.getLogger(Manager.class.getName());

	/**
	* Interval timer is running.
	*/
	public static final int TICK_INTERVAL = 40;

	/**
	 * A list of mascot.
	 */
	private final List<Mascot> mascots = new ArrayList<Mascot>();

	/**
     * AI 对话服务实例
     * OCP: 依赖于抽象 AiChatService
     */
private final MemoryManager memoryManager = new MemoryManager();
private final AiChatService aiService = new DeepseekChatService(memoryManager);

/**
	 * 为指定的桌宠创建右键弹出菜单。
	 * @param mascot 需要创建菜单的桌宠实例
	 * @return 创建好的 PopupMenu 对象
	 */
	private PopupMenu constructMenu(final Mascot mascot) {
		
        final PopupMenu menu = new PopupMenu(); // 创建一个新的弹出菜单

        // --- AI 对话菜单项 ---
        final MenuItem aiChatItem = new MenuItem(Main.getInstance().getLanguageBundle().getString("ChatWithAI")); 
        
        aiChatItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                try {
                    // 获取当前桌宠使用的 Configuration（包含 schema）
                    final Configuration config = Main.getInstance().getConfiguration(mascot.getImageSet());
                    final java.util.ResourceBundle schema = config.getSchema();

                    // 创建一个空的动画列表（如果需要可从 config/资源加载真实动画）
                    final java.util.List<com.group_finity.mascot.animation.Animation> animations = new java.util.ArrayList<com.group_finity.mascot.animation.Animation>();

                    // 创建参数 map
                    final com.group_finity.mascot.script.VariableMap params = new com.group_finity.mascot.script.VariableMap();

                    // 复用已有的 AI 服务实例
                    final com.group_finity.mascot.ai.AiChatService aiService = Manager.this.aiService;

                    // 使用与 ChatAction 构造器匹配的参数
                    com.group_finity.mascot.action.Action chatAction = new com.group_finity.mascot.action.ChatAction(
                        schema,
                        animations,
                        params,
                        aiService
                    );

                    // 使用名字 "Chat" 和当前 configuration 构造 UserBehavior（与其构造器签名匹配）
                    mascot.setBehavior(new com.group_finity.mascot.behavior.UserBehavior(
                        "Chat",
                        chatAction,
                        config
                    ));
                    
                } catch (final Exception ex) {
                    log.log(Level.WARNING, "无法启动 AI 聊天", ex);
                }
            }
        });
        menu.add(aiChatItem); // 将 AI 对话菜单项添加到菜单中
        // --- AI 菜单项结束 ---

        return menu; // 返回创建好的菜单
	}

	
	/**
	* The mascot will be added later.
	* (@Link ConcurrentModificationException) to prevent the addition of the mascot (@link # tick ()) are each simultaneously reflecting.
	 */
	private final Set<Mascot> added = new LinkedHashSet<Mascot>();

	/**
	* The mascot will be added later.
	* (@Link ConcurrentModificationException) to prevent the deletion of the mascot (@link # tick ()) are each simultaneously reflecting.
	 */
	private final Set<Mascot> removed = new LinkedHashSet<Mascot>();

	private boolean exitOnLastRemoved = true;
	
	private Thread thread;

	public void setExitOnLastRemoved(boolean exitOnLastRemoved) {
		this.exitOnLastRemoved = exitOnLastRemoved;
	}

	public boolean isExitOnLastRemoved() {
		return exitOnLastRemoved;
	}

	public Manager() {

		new Thread() {
			{
				this.setDaemon(true);
				this.start();
			}

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(Integer.MAX_VALUE);
					} catch (final InterruptedException ex) {
					}
				}
			}
		};
	}
	
	public void start() {
		if ( thread!=null && thread.isAlive() ) {
			return;
		}
		
		thread = new Thread() {
			@Override
			public void run() {

				long prev = System.nanoTime() / 1000000;
				try {
					for (;;) {
						for (;;) {
							final long cur = System.nanoTime() / 1000000;
							if (cur - prev >= TICK_INTERVAL) {
								if (cur > prev + TICK_INTERVAL * 2) {
									prev = cur;
								} else {
									prev += TICK_INTERVAL;
								}
								break;
							}
							Thread.sleep(1, 0);
						}

						tick();
					}
				} catch (final InterruptedException e) {
				}
			}
		};
		thread.setDaemon(false);
		
		thread.start();
	}
	
	public void stop() {
		if ( thread==null || !thread.isAlive() ) {
			return;
		}
		thread.interrupt();
		try {
			thread.join();
		} catch (InterruptedException e) {
		}
	}

	private void tick( )
        {
            // Update the first environmental information
            NativeFactory.getInstance().getEnvironment().tick();

		synchronized (this.getMascots()) {

			// Add the mascot if it should be added
			for (final Mascot mascot : this.getAdded()) {
				this.getMascots().add(mascot);
			}
			this.getAdded().clear();

			// Remove the mascot if it should be removed
			for (final Mascot mascot : this.getRemoved()) {
				this.getMascots().remove(mascot);
			}
			this.getRemoved().clear();

			// Advance mascot's time
			for (final Mascot mascot : this.getMascots()) {
				mascot.tick();
			}
			
			// Advance mascot's time
			for (final Mascot mascot : this.getMascots()) {
				mascot.apply();
			}
		}

		if (isExitOnLastRemoved()) {
			if (this.getMascots().size() == 0) {
				Main.getInstance().exit();
			}
		}
	}

	public void add(final Mascot mascot) {
        synchronized (this.getAdded()) {
            this.getAdded().add(mascot);
            this.getRemoved().remove(mascot);
        }
        mascot.setManager(this);

        // NOTE: 不在 Manager 中访问 mascot.getWindow()（该方法是 private）
        // 原来在这里添加鼠标监听并显示自定义弹出菜单的代码已移入 Mascot.showPopup()
    }

	public void remove(final Mascot mascot) {
		synchronized (this.getAdded()) {
			this.getAdded().remove(mascot);
			this.getRemoved().add(mascot);
		}
		mascot.setManager(null);
	}

	public void setBehaviorAll(final String name) {
		synchronized (this.getMascots()) {
			for (final Mascot mascot : this.getMascots()) {
				try {
                                    Configuration configuration = Main.getInstance( ).getConfiguration( mascot.getImageSet( ) );
				    mascot.setBehavior( configuration.buildBehavior( configuration.getSchema( ).getString( name ), mascot ) );
				} catch (final BehaviorInstantiationException e) {
					log.log(Level.SEVERE, "Failed to initialize the following actions", e);
					Main.showError( Main.getInstance( ).getLanguageBundle( ).getString( "FailedSetBehaviourErrorMessage" ), e );
					mascot.dispose();
				} catch (final CantBeAliveException e) {
					log.log(Level.SEVERE, "Fatal Error", e);
                                        Main.showError( Main.getInstance( ).getLanguageBundle( ).getString( "FailedSetBehaviourErrorMessage" ), e );
					mascot.dispose();
				}
			}
		}
	}	
	
	public void setBehaviorAll(final Configuration configuration, final String name, String imageSet) {
		synchronized (this.getMascots()) {
			for (final Mascot mascot : this.getMascots()) {
				try {
					if( mascot.getImageSet().equals(imageSet) ) {
						mascot.setBehavior(configuration.buildBehavior( configuration.getSchema( ).getString( name ), mascot ) );						
					}
				} catch (final BehaviorInstantiationException e) {
					log.log(Level.SEVERE, "Failed to initialize the following actions", e);
					Main.showError( Main.getInstance( ).getLanguageBundle( ).getString( "FailedSetBehaviourErrorMessage" ), e );
					mascot.dispose();
				} catch (final CantBeAliveException e) {
					log.log(Level.SEVERE, "Fatal Error", e);
					Main.showError( Main.getInstance( ).getLanguageBundle( ).getString( "FailedSetBehaviourErrorMessage" ), e );
					mascot.dispose();
				}
			}
		}
	}

	public void remainOne() {
		synchronized (this.getMascots()) {
			int totalMascots = this.getMascots().size();
			for (int i = totalMascots - 1; i > 0; --i) {
				this.getMascots().get(i).dispose();				
			}
		}
        }
        
	public void remainOne( Mascot mascot )
        {
            synchronized( this.getMascots( ) )
            {
                int totalMascots = this.getMascots( ).size( );
                for( int i = totalMascots - 1; i >= 0; --i )
                {
                    if( !this.getMascots( ).get( i ).equals( mascot ) )
                        this.getMascots( ).get( i ).dispose( );
                }
            }
	}
	
	public void remainOne( String imageSet ) {
		synchronized (this.getMascots()) {
			int totalMascots = this.getMascots().size();
			boolean isFirst = true;
			for (int i = totalMascots - 1; i >= 0; --i) {
				Mascot m = this.getMascots().get(i);
				if( m.getImageSet().equals(imageSet) && isFirst) {
					isFirst = false;
				} else if( m.getImageSet().equals(imageSet) && !isFirst) {
					m.dispose();
				}
			}
		}
	}
	
    public void remainNone( String imageSet )
    {
        synchronized( this.getMascots( ) )
        {
            int totalMascots = this.getMascots( ).size( );
            for( int i = totalMascots - 1; i >= 0; --i )
            {
                Mascot m = this.getMascots( ).get( i );
                if( m.getImageSet( ).equals( imageSet ) )
                    m.dispose( );
            }
        }
    }

    public void togglePauseAll( )
    {
        boolean isPaused = true;
        
        synchronized( this.getMascots( ) )
        {
            for( final Mascot mascot : this.getMascots( ) )
            {
                if( !mascot.isPaused( ) )
                {
                    isPaused = false;
                    break;
                }
            }
            
            for( final Mascot mascot : this.getMascots( ) )
            {
                mascot.setPaused( !isPaused );
            }
        }
    }

    public boolean isPaused( )
    {
        boolean isPaused = true;
        
        synchronized( this.getMascots( ) )
        {
            for( final Mascot mascot : this.getMascots( ) )
            {
                if( !mascot.isPaused( ) )
                {
                    isPaused = false;
                    break;
                }
            }
        }
        
        return isPaused;
    }

    public int getCount( )
    {
        return getCount( null );
    }
    
    public int getCount( String imageSet )
    {
        synchronized( getMascots( ) )
        {
            if( imageSet == null )
            {
                return getMascots( ).size( );
            }
            else   
            {
                int count = 0;
                for( int index = 0; index < getMascots( ).size( ); index++ )
                {
                    Mascot m = getMascots( ).get( index );
                    if( m.getImageSet( ).equals( imageSet ) )
                        count++;
                }
                return count;
            }
        }
    }

	private List<Mascot> getMascots() {
		return this.mascots;
	}

	private Set<Mascot> getAdded() {
		return this.added;
	}

	private Set<Mascot> getRemoved() {
		return this.removed;
	}
        
        /**
         * Returns a Mascot with the given affordance.
         * @param affordance
         * @return A WeakReference to a mascot with the required affordance, or null
         */
        public WeakReference<Mascot> getMascotWithAffordance( String affordance )
        {
            synchronized( this.getMascots( ) )
            {
                for( final Mascot mascot : this.getMascots( ) )
                {
                    if( mascot.getAffordances( ).contains( affordance ) )
                        return new WeakReference<Mascot>( mascot );
                }
            }
            
            return null;
        }

    public boolean hasOverlappingMascotsAtPoint( Point anchor )
    {
        int count = 0;
        
        synchronized( this.getMascots( ) )
        {
            for( final Mascot mascot : this.getMascots( ) )
            {
                if( mascot.getAnchor( ).equals( anchor ) )
                    count++;
                if( count > 1 )
                    return true;
            }
        }

        return false;
    }

	public void disposeAll() {
		synchronized (this.getMascots()) {
			for (int i = this.getMascots().size() - 1; i >= 0; --i) {
				this.getMascots().get(i).dispose();
			}
		}
	}
}
