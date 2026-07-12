import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JPanel;

/**
 * 游戏主面板类，负责游戏逻辑、物理引擎、渲染和用户交互
 * 
 * 【整体流程】
 * 1. 用户移动鼠标控制预览球位置
 * 2. 用户点击释放预览球
 * 3. 球受重力影响下落
 * 4. 球与墙壁、其他球发生碰撞
 * 5. 相同等级的球合并
 * 6. 球稳定后进入下一回合
 * 7. 球越过红线触发游戏结束
 * 
 * 【与合成大西瓜的差距】
 * 1. 缺少真实物理引擎（如Box2D），物理模拟精度有限
 * 2. 球体堆叠稳定性不如原版，容易出现抖动
 * 3. 缺少流体力学效果（球的形变）
 * 4. 缺少音效系统
 * 5. 缺少粒子特效
 * 
 * 【常见异常问题及修复位置】
 * 1. 球飞起来：检查isBallSupported()和findRestY()逻辑
 * 2. 球不挤压：检查separateBalls()中的分离份额分配
 * 3. 球抖动：调整SETTLE_VELOCITY阈值和FRICTION系数
 * 4. 回合卡住：检查isSceneQuiescent()和updateTurnState()逻辑
 */
public class GamePanel extends JPanel {
    
    // ==================== 常量定义 ====================
    
    /** 游戏画布宽度 */
    public static final int WIDTH = 600;
    /** 游戏画布高度 */
    public static final int HEIGHT = 600;

        /** 重力加速度（每帧增加的垂直速度） */
    private static final double GRAVITY = 0.5;
    /** 空气摩擦系数（每帧水平速度衰减比例） */
    private static final double FRICTION = 0.99;
    /** 墙壁反弹系数（碰撞后速度保留比例） */
    private static final double WALL_RESTITUTION = 0.05;
    /** 地面反弹系数（碰撞后速度保留比例） */
    private static final double GROUND_RESTITUTION = 0.15;
    /** 球体碰撞反弹系数（碰撞后速度保留比例） */
    private static final double BALL_RESTITUTION = 0.05;
    /** 最小地面反弹速度（低于此值不反弹） */
    private static final double MIN_GROUND_BOUNCE = 0.3;
    /** 触发合并前允许的最大速度阈值 */
    private static final double MERGE_SPEED_LIMIT = 1.5;
    /** 触发合并前允许的最大距离误差 */
    private static final double MERGE_DISTANCE_TOLERANCE = 2.0;
    
    /** 球的生成权重，索引为等级，值越大生成概率越高 */
    private static final int[] SPAWN_WEIGHTS = {4, 3, 2, 1};
    /** 顶部边距（预览球的最低Y位置） */
    private static final int TOP_MARGIN = 15;
    /** 红线Y坐标（球越过此线触发游戏结束） */
    private static final int RED_LINE_Y = 80;
    /** 物理求解器迭代次数（每帧碰撞检测迭代次数） */
    private static final int SOLVER_ITERATIONS = 5;
    /** 稳定速度阈值（低于此值认为球已停止） */
    private static final double SETTLE_VELOCITY = 0.4;
    /** 静止帧数要求（连续多少帧静止后结束回合） */
    private static final int QUIESCENT_FRAMES_REQUIRED = 3;
    /** 碰撞检测阈值（两球距离小于此值才触发碰撞） */
    private static final double COLLISION_THRESHOLD = 0.1;
    /** 最大回合帧数（超过此帧数强制结束回合，防止卡住），约1.5秒（60fps） */
    private static final int MAX_TURN_FRAMES = 90;
    /** 静止球分离时最多承受的分离比例，防止抬升地基球 */
    private static final double MAX_STATIC_SHARE = 0.15;
    

    // ==================== 成员变量 ====================
    
    /** 当前场上所有球体列表 */
    private final List<Ball> balls = new ArrayList<>();
    /** 当前正在进行的合并动画列表 */
    private final List<MergeAnimation> mergeAnimations = new ArrayList<>();
    /** 随机数生成器（用于球的等级生成） */
    private final Random random = new Random();
    
    /** 预览球（鼠标控制的待释放球） */
    private Ball previewBall;
    /** 当前关注的球（刚释放的球，用于追踪状态） */
    private Ball watchedBall;
    
    /** 当前得分 */
    private int score;
    /** 是否处于回合中（球正在下落/合并） */
    private boolean turnInProgress;
    /** 连续静止帧数（用于判断回合结束） */
    private int quiescentFrames;
    /** 当前回合已进行帧数（用于超时判断） */
    private int turnFrames;
    /** 本帧是否发生了合并（用于延迟回合结束） */
    private boolean mergeOccurredThisFrame;
    /** 游戏是否结束 */
    private boolean gameOver;
    
    /** 游戏过程中达到的最高球等级 */
    private int maxLevelReached;
    /** 游戏过程中投放的球总数 */
    private int totalBallsSpawned;
    /** 游戏过程中的合并次数 */
    private int totalMerges;
    /** 是否显示恭喜界面（生成终极球时显示） */
    private boolean showCongrats;

    // ==================== 构造函数 ====================
    
    /**
     * 构造函数：初始化游戏面板
     * 
     * 【流程】
     * 1. 设置面板尺寸和背景色
     * 2. 添加鼠标移动监听（控制预览球位置）
     * 3. 添加鼠标点击监听（释放球/重新开始）
     * 4. 生成第一个预览球
     * 
     * 【边界条件】
     * - 面板尺寸固定为600x600
     * - 背景色为深色主题
     */
    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(30, 30, 40));

        // 鼠标移动监听：更新预览球位置
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updatePreviewPosition(e.getX());
            }
        });

        // 鼠标点击监听：释放球或重新开始
        // 使用 mousePressed 代替 mouseClicked，因为 mouseClicked 需要完整的按下-释放周期
        // 如果用户按下后有轻微移动，mouseClicked 可能不会触发，导致点击失效
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (showCongrats) {
                    showCongrats = false;
                } else if (gameOver) {
                    restartGame();
                } else {
                    releasePreviewBall();
                }
            }
        });

        // 键盘监听：ESC键退出游戏并计算分数
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (!gameOver) {
                        gameOver = true;
                    }
                }
            }
        });
        // 设置面板可获取焦点，确保键盘事件能被捕获
        setFocusable(true);
        requestFocusInWindow();

        // 生成第一个预览球
        prepareNextPreview();
    }

    // ==================== 音效系统 ====================
    
    /**
     * 播放音效（使用Java Sound API生成简单音效）
     * 
     * 【参数】
     * @param frequency 频率（Hz），决定音高
     * @param duration 持续时间（毫秒）
     * @param volume 音量（0.0-1.0）
     * 
     * 【实现原理】
     * 使用正弦波生成音效，通过SourceDataLine输出
     * 采用新线程播放，避免阻塞游戏主循环
     * 
     * 【设计意图】
     * - 不依赖外部音频文件，纯代码生成
     * - 简单高效，适合游戏音效
     */
    private void playSound(double frequency, int duration, double volume) {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();
                
                int sampleCount = (int) (format.getSampleRate() * duration / 1000);
                byte[] samples = new byte[sampleCount * 2];
                
                for (int i = 0; i < sampleCount; i++) {
                    double angle = 2.0 * Math.PI * frequency * i / format.getSampleRate();
                    short sample = (short) (Math.sin(angle) * volume * Short.MAX_VALUE);
                    samples[i * 2] = (byte) (sample & 0xFF);
                    samples[i * 2 + 1] = (byte) (sample >> 8);
                }
                
                line.write(samples, 0, samples.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                // 忽略音效播放错误
            }
        }).start();
    }

    /**
     * 播放释放球的音效
     * 
     * 【音效参数】
     * - 频率：400Hz开始，快速下降到200Hz
     * - 模拟球下落的"嗖"声
     */
    private void playReleaseSound() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();
                
                int duration = 150;
                int sampleCount = (int) (format.getSampleRate() * duration / 1000);
                byte[] samples = new byte[sampleCount * 2];
                
                for (int i = 0; i < sampleCount; i++) {
                    double progress = (double) i / sampleCount;
                    double frequency = 400 - progress * 200;
                    double angle = 2.0 * Math.PI * frequency * i / format.getSampleRate();
                    double envelope = 1.0 - progress;
                    short sample = (short) (Math.sin(angle) * 0.3 * envelope * Short.MAX_VALUE);
                    samples[i * 2] = (byte) (sample & 0xFF);
                    samples[i * 2 + 1] = (byte) (sample >> 8);
                }
                
                line.write(samples, 0, samples.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                // 忽略音效播放错误
            }
        }).start();
    }

    /**
     * 播放合成球的音效
     * 
     * 【音效参数】
     * - 频率：从低到高的上升音
     * - 模拟合成的"叮"声
     */
    private void playMergeSound() {
        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();
                
                int duration = 200;
                int sampleCount = (int) (format.getSampleRate() * duration / 1000);
                byte[] samples = new byte[sampleCount * 2];
                
                for (int i = 0; i < sampleCount; i++) {
                    double progress = (double) i / sampleCount;
                    double frequency = 300 + progress * 400;
                    double angle = 2.0 * Math.PI * frequency * i / format.getSampleRate();
                    double envelope = Math.sin(progress * Math.PI);
                    short sample = (short) (Math.sin(angle) * 0.4 * envelope * Short.MAX_VALUE);
                    samples[i * 2] = (byte) (sample & 0xFF);
                    samples[i * 2 + 1] = (byte) (sample >> 8);
                }
                
                line.write(samples, 0, samples.length);
                line.drain();
                line.close();
            } catch (Exception e) {
                // 忽略音效播放错误
            }
        }).start();
    }

    // ==================== 预览球逻辑 ====================
    
    /**
     * 获取预览球的Y坐标
     * 
     * 【逻辑】
     * 预览球位置必须满足两个条件：
     * 1. 球顶部不低于TOP_MARGIN（顶部边距）
     * 2. 球顶部不低于RED_LINE_Y（红线位置）
     * 取两者中的较大值，确保预览球不会越过红线
     * 
     * 【边界条件】
     * - 当球半径较大时（如140），Y坐标会更高
     * - 防止预览球初始位置触发游戏结束
     */
    private int getPreviewY(int radius) {
        return Math.max(TOP_MARGIN + radius, RED_LINE_Y + radius);
    }

    /**
     * 更新预览球位置
     * 
     * 【流程】
     * 1. 如果没有预览球，直接返回
     * 2. 限制X坐标在画布范围内（不超出左右边界）
     * 3. 设置预览球的X和Y坐标
     * 
     * 【边界条件】
     * - X坐标范围：[radius, WIDTH - radius]
     * - Y坐标固定：getPreviewY(radius)
     */
    private void updatePreviewPosition(int mouseX) {
        if (previewBall == null) {
            return;
        }

        int radius = previewBall.getRadius();
        int x = Math.max(radius, Math.min(WIDTH - radius, mouseX));
        previewBall.setX(x);
        previewBall.setY(getPreviewY(radius));
    }

    /**
     * 准备下一个预览球
     * 
     * 【流程】
     * 1. 随机生成球的等级（根据SPAWN_WEIGHTS权重）
     * 2. 获取该等级对应的半径
     * 3. 创建新的预览球，位置在屏幕中央顶部
     * 
     * 【边界条件】
     * - 等级范围：0-3（对应SPAWN_WEIGHTS数组长度）
     * - 初始X位置：WIDTH / 2（屏幕中央）
     */
    private void prepareNextPreview() {
        int spawnLevel = randomSpawnLevel();
        int radius = Ball.getRadiusForLevel(spawnLevel);
        previewBall = new Ball(WIDTH / 2.0, getPreviewY(radius), spawnLevel);
    }

    // ==================== 球释放逻辑 ====================
    
    /**
     * 释放预览球
     * 
     * 【流程】
     * 1. 检查是否可以释放（有预览球且不在回合中）
     * 2. 获取预览球的位置和半径
     * 3. 检查释放位置是否与已有球重叠（防止卡在已有球中）
     * 4. 创建新球并添加到场上
     * 5. 重置预览球和回合状态
     * 6. 更新统计数据
     * 
     * 【边界条件】
     * - 释放位置不能与已有球重叠
     * - 只能在回合结束后释放新球
     * 
     * 【异常处理】
     * - 如果释放位置与已有球重叠，不释放并等待用户重新调整位置
     */
    private void releasePreviewBall() {
        // 边界检查：没有预览球或正在回合中，不能释放
        if (previewBall == null || turnInProgress) {
            return;
        }

        int radius = previewBall.getRadius();
        double x = previewBall.getX();
        double y = previewBall.getY();

        // 检查释放位置是否与已有球重叠
        for (Ball ball : balls) {
            if (ball.isMarkedForRemoval() || ball.isMerging()) {
                continue;
            }
            double dx = ball.getX() - x;
            double dy = ball.getY() - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            // 如果距离小于两球半径之和，说明重叠，不释放
            if (dist < ball.getRadius() + radius) {
                return;
            }
        }

        // 创建新球并添加到场上
        watchedBall = new Ball(x, y, previewBall.getLevel());
        balls.add(watchedBall);
        
        // 播放释放球音效
        playReleaseSound();
        
        // 重置预览球和回合状态
        previewBall = null;
        turnInProgress = true;
        quiescentFrames = 0;
        turnFrames = 0;
        
        // 更新统计数据
        totalBallsSpawned++;
        maxLevelReached = Math.max(maxLevelReached, watchedBall.getLevel());
    }

    /**
     * 随机生成球的等级（权重随机）
     * 
     * 【算法】
     * 加权随机算法：
     * 1. 计算总权重（4+3+2+1=10）
     * 2. 生成0到总权重-1的随机数
     * 3. 根据累加权重判断落在哪个等级区间
     * 
     * 【概率分布】
     * - 等级0（最小球）：4/10 = 40%
     * - 等级1：3/10 = 30%
     * - 等级2：2/10 = 20%
     * - 等级3：1/10 = 10%
     * 
     * 【边界条件】
     * - 返回值范围：0到SPAWN_WEIGHTS.length-1
     * - 如果数组为空，默认返回0
     */
    private int randomSpawnLevel() {
        int totalWeight = 0;
        for (int weight : SPAWN_WEIGHTS) {
            totalWeight += weight;
        }

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (int level = 0; level < SPAWN_WEIGHTS.length; level++) {
            cumulative += SPAWN_WEIGHTS[level];
            if (roll < cumulative) {
                return level;
            }
        }
        return 0;
    }

    // ==================== 主更新循环 ====================
    
    /**
     * 每帧更新逻辑（游戏主循环）
     * 
     * 【执行顺序】
     * 1. 检查游戏是否结束
     * 2. 重置合并标志
     * 3. 更新合并动画
     * 4. 应用重力
     * 5. 迭代求解碰撞（墙壁+球体）
     * 6. 处理合并
     * 7. 移除已合并的球
     * 8. 检查红线碰撞
     * 9. 更新回合状态
     * 
     * 【关键设计】
     * - 碰撞求解器迭代多次（SOLVER_ITERATIONS=8），提高稳定性
     * - 合并检测在碰撞之后，确保只有稳定接触的球才会合并
     * - 红线检查在所有物理更新之后，确保位置准确
     */
    public void update() {
        // 游戏结束后不再更新
        if (gameOver) {
            return;
        }

        // 重置本帧合并标志
        mergeOccurredThisFrame = false;

        // 更新合并动画（可能生成新球）
        updateMergeAnimations();
        
        // 应用重力和摩擦
        applyGravity();

        // 迭代求解碰撞（多次迭代提高稳定性）
        for (int i = 0; i < SOLVER_ITERATIONS; i++) {
            resolveWallCollisions();
            resolveBallCollisions();
        }

        // 快速吸附稳定：球速度低且接近支撑位置时，直接归零速度并吸附到支撑位置
        snapToRest();

        // 处理球的合并
        resolveMerges();
        
        // 移除标记为删除的球
        removeMergedBalls();
        
        // 检查是否有球越过红线
        checkRedLineCollision();
        
        // 更新回合状态（计数和结束判断）
        if (turnInProgress) {
            turnFrames++;
        }
        updateTurnState();
    }

    // ==================== 合并动画 ====================
    
    /**
     * 更新合并动画
     * 
     * 【流程】
     * 1. 遍历所有合并动画
     * 2. 更新动画进度
     * 3. 如果动画完成：
     *    - 创建合并结果球
     *    - 完成动画（标记原球为删除）
     *    - 将结果球添加到场上
     *    - 更新得分和统计
     *    - 更新关注球（如果关注球参与了合并）
     *    - 标记本帧发生了合并
     *    - 重置静止帧数
     *    - 移除已完成的动画
     * 
     * 【边界条件】
     * - 动画完成后必须移除，防止重复处理
     * - 关注球可能变为合并结果球
     */
    private void updateMergeAnimations() {
        Iterator<MergeAnimation> it = mergeAnimations.iterator();
        while (it.hasNext()) {
            MergeAnimation animation = it.next();
            if (animation.update()) {
                // 动画完成，创建结果球
                Ball result = animation.createResultBall();
                animation.finish();
                
                // 将结果球添加到场上
                balls.add(result);
                
                // 更新得分和统计
                score += result.getMergeScore();
                totalMerges++;
                maxLevelReached = Math.max(maxLevelReached, result.getLevel());

                // 检查是否生成了终极球（等级6）
                if (result.isUltimateLevel()) {
                    showCongrats = true;
                }

                // 如果关注球参与了合并，更新关注球
                if (animation.involves(watchedBall)) {
                    watchedBall = result;
                }

                // 标记本帧发生了合并，延迟回合结束
                mergeOccurredThisFrame = true;
                quiescentFrames = 0;
                
                // 移除已完成的动画
                it.remove();
            }
        }
    }

    // ==================== 回合状态管理 ====================
    
    /**
     * 更新回合状态
     * 
     * 【流程】
     * 1. 如果不在回合中，直接返回
     * 2. 检查关注球是否有效（未被删除且仍在场上）
     * 3. 检查是否超时（超过MAX_TURN_FRAMES强制结束）
     * 4. 检查场景是否静止：
     *    - 如果静止，增加静止帧数
     *    - 如果静止帧数达到要求，结束回合
     *    - 如果不静止，重置静止帧数
     * 
     * 【边界条件】
     * - 回合超时：防止无限等待球稳定
     * - 关注球失效：合并后关注球变为结果球
     * 
     * 【异常处理】
     * - 强制结束回合（forceEndTurn）：处理球卡在半空的情况
     */
    private void updateTurnState() {
        if (!turnInProgress) {
            return;
        }

        // 检查关注球是否有效
        if (watchedBall != null
                && (watchedBall.isMarkedForRemoval() || !balls.contains(watchedBall))) {
            watchedBall = null;
        }

        // 超时检查：强制结束回合
        if (turnFrames >= MAX_TURN_FRAMES) {
            forceEndTurn();
            return;
        }

        // 检查场景是否静止
        if (isSceneQuiescent()) {
            quiescentFrames++;
            // 连续静止足够帧数后结束回合
            if (quiescentFrames >= QUIESCENT_FRAMES_REQUIRED) {
                turnInProgress = false;
                watchedBall = null;
                // 生成下一个预览球
                if (previewBall == null) {
                    prepareNextPreview();
                }
            }
        } else {
            // 场景未静止，重置静止帧数
            quiescentFrames = 0;
        }
    }

    /**
     * 强制结束回合（超时或异常情况）
     * 
     * 【流程】
     * 1. 重置回合状态标志
     * 2. 重置静止帧数和回合帧数
     * 3. 如果没有预览球，生成下一个
     * 
     * 【使用场景】
     * - 球卡在半空无法稳定（如被多个球夹住）
     * - 物理引擎计算异常导致球永远不静止
     */
    private void forceEndTurn() {
        turnInProgress = false;
        watchedBall = null;
        quiescentFrames = 0;
        turnFrames = 0;
        if (previewBall == null) {
            prepareNextPreview();
        }
    }

    /**
     * 检查场景是否静止（所有球都已稳定）
     * 
     * 【判断条件】
     * 1. 本帧没有发生合并
     * 2. 没有正在进行的合并动画
     * 3. 所有球都已稳定（isBallSettled返回true）
     * 
     * 【边界条件】
     * - 合并过程中场景不会静止
     * - 球正在运动时场景不会静止
     */
    private boolean isSceneQuiescent() {
        // 合并过程中场景不静止
        if (mergeOccurredThisFrame || !mergeAnimations.isEmpty()) {
            return false;
        }

        // 检查所有球是否都已稳定
        for (Ball ball : balls) {
            if (ball.isMarkedForRemoval() || ball.isMerging()) {
                continue;
            }
            if (!isBallSettled(ball)) {
                return false;
            }
        }
        return true;
    }

    // ==================== 稳定判定 ====================
    
    /**
     * 判断球是否已稳定（停止运动且被支撑）
     * 
     * 【判定条件】
     * 1. 速度低于SETTLE_VELOCITY阈值（0.15）
     * 2. 球被其他球或地面支撑（isBallSupported返回true）
     * 
     * 【设计意图】
     * - 不仅仅看速度，还要看是否被支撑
     * - 即使速度为0，如果球悬在空中，也不算稳定
     * - 这意味着球被其他球挡住无法继续下落时就算稳定
     * 
     * 【与合成大西瓜的差异】
     * - 原版可能使用更复杂的稳定判定（如睡眠状态）
     * - 本实现较简单，可能导致球在微小振动时误判
     */
    private boolean isBallSettled(Ball ball) {
        // 速度检查：速度高于阈值则未稳定
        double speed = Math.hypot(ball.getVx(), ball.getVy());
        if (speed >= SETTLE_VELOCITY) {
            return false;
        }
        // 支撑检查：球是否被支撑
        return isBallSupported(ball);
    }

    /**
     * 判断球是否被支撑（无法继续下落）
     * 
     * 【算法】
     * 1. 计算球的最低可能Y位置（restY）
     * 2. 遍历其他所有球，计算它们能提供的支撑位置
     * 3. 如果球的当前Y位置与最低可能位置接近（±2像素），则认为被支撑
     * 
     * 【支撑来源】
     * - 地面（HEIGHT - radius）
     * - 其他球（通过圆心距计算）
     * 
     * 【边界条件】
     * - 合并中的球视为已支撑
     * - 只考虑非删除、非合并的球作为支撑来源
     * - 水平距离超过两球半径之和+0.5时，不提供支撑
     * 
     * 【常见问题】
     * - 如果支撑位置计算错误，会导致球被错误地判定为稳定或不稳定
     * - 新释放的球如果被当作支撑来源，会导致已有球飞起来
     */
    private boolean isBallSupported(Ball ball) {
        // 合并中的球视为已支撑
        if (ball.isMerging()) {
            return true;
        }
        
        // 默认支撑位置为地面
        double restY = HEIGHT - ball.getRadius();
        
        // 遍历其他球，计算可能的支撑位置
        for (Ball other : balls) {
            if (other == ball || other.isMarkedForRemoval() || other.isMerging()) {
                continue;
            }
            
            // 计算两球水平距离
            double dx = ball.getX() - other.getX();
            double sumR = ball.getRadius() + other.getRadius();
            
            // 水平距离太远，不提供支撑
            if (Math.abs(dx) >= sumR + 0.5) {
                continue;
            }
            
            // 计算垂直方向的支撑位置
            double dy = Math.sqrt(sumR * sumR - dx * dx);
            double candidateY = other.getY() - dy;
            
            // 更新最低支撑位置
            if (candidateY < restY) {
                restY = candidateY;
            }
        }
        
        // 判断球是否在支撑位置附近（±2像素容差）
        double diff = ball.getY() - restY;
        return diff >= -2.0 && diff <= 2.0;
    }

    // ==================== 物理辅助方法 ====================
    
    /**
     * 判断球是否正在合并动画中
     * 
     * 【用途】
     * - 合并动画中的球不参与物理计算
     * - 防止合并过程中球被其他球推动
     */
    private boolean isInMergeAnimation(Ball ball) {
        for (MergeAnimation animation : mergeAnimations) {
            if (animation.involves(ball)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断球是否参与物理模拟
     * 
     * 【条件】
     * 1. 未被标记为删除
     * 2. 不在合并中
     * 3. 不在合并动画中
     * 
     * 【用途】
     * - 过滤掉不需要参与物理计算的球
     * - 防止已删除或正在合并的球影响其他球
     */
    private boolean isPhysicsActive(Ball ball) {
        return !ball.isMarkedForRemoval() && !ball.isMerging() && !isInMergeAnimation(ball);
    }

    // ==================== 物理引擎 ====================
    
    /**
     * 应用重力和摩擦
     * 
     * 【流程】
     * 1. 遍历所有参与物理的球
     * 2. 垂直方向：增加重力速度
     * 3. 水平方向：应用摩擦（速度衰减）
     * 4. 根据速度更新球的位置
     * 
     * 【边界条件】
     * - 只对物理活跃的球生效
     * - 摩擦只影响水平速度，不影响垂直速度
     * 
     * 【与合成大西瓜的差异】
     * - 原版可能有更复杂的物理模型（如质量、惯性）
     * - 本实现所有球使用相同的重力和摩擦
     */
    private void applyGravity() {
        for (Ball ball : balls) {
            if (!isPhysicsActive(ball)) {
                continue;
            }

            // 垂直方向：增加重力
            ball.setVy(ball.getVy() + GRAVITY);
            
            // 水平方向：应用摩擦
            ball.setVx(ball.getVx() * FRICTION);
            
            // 更新位置
            ball.setX(ball.getX() + ball.getVx());
            ball.setY(ball.getY() + ball.getVy());
        }
    }

    /**
     * 处理球与墙壁的碰撞
     * 
     * 【流程】
     * 1. 遍历所有参与物理的球
     * 2. 检查左右墙壁碰撞：
     *    - 左墙：x - radius < 0
     *    - 右墙：x + radius > WIDTH
     *    - 处理：重置位置到边界，反转水平速度并应用反弹系数
     * 3. 检查上下边界碰撞：
     *    - 上边界：y - radius < 0
     *    - 下边界（地面）：y + radius > HEIGHT
     *    - 处理：重置位置到边界，处理反弹
     * 
     * 【边界条件】
     * - 墙壁碰撞只改变水平速度
     * - 地面碰撞会触发bounceGround（可能停止反弹）
     * 
     * 【常见问题】
     * - 如果墙壁反弹系数过大，球会在墙壁间来回弹动
     * - 如果地面反弹处理不当，球可能永远弹动不止
     */
    private void resolveWallCollisions() {
        for (Ball ball : balls) {
            if (!isPhysicsActive(ball)) {
                continue;
            }

            int r = ball.getRadius();
            double x = ball.getX();
            double y = ball.getY();

            // 左墙壁碰撞
            if (x - r < 0) {
                ball.setX(r);
                ball.setVx(-ball.getVx() * WALL_RESTITUTION);
            } 
            // 右墙壁碰撞
            else if (x + r > WIDTH) {
                ball.setX(WIDTH - r);
                ball.setVx(-ball.getVx() * WALL_RESTITUTION);
            }

            // 上边界碰撞
            if (y - r < 0) {
                ball.setY(r);
                ball.setVy(-ball.getVy() * WALL_RESTITUTION);
            } 
            // 地面碰撞
            else if (y + r > HEIGHT) {
                ball.setY(HEIGHT - r);
                if (ball.getVy() > 0) {
                    bounceGround(ball);
                }
            }
        }
    }

    /**
     * 处理地面反弹
     * 
     * 【流程】
     * 1. 计算反弹后的垂直速度
     * 2. 如果反弹速度太小（低于MIN_GROUND_BOUNCE），停止反弹
     * 3. 否则应用反弹速度
     * 4. 应用水平摩擦
     * 
     * 【设计意图】
     * - 防止球在地面无限弹动
     * - 当反弹能量不足时，球会停止
     * 
     * 【边界条件】
     * - 最小反弹速度：MIN_GROUND_BOUNCE = 0.25
     * - 低于此值时，球直接停止
     */
    private void bounceGround(Ball ball) {
        double bouncedVy = -ball.getVy() * GROUND_RESTITUTION;
        if (Math.abs(bouncedVy) < MIN_GROUND_BOUNCE) {
            ball.setVy(0);
        } else {
            ball.setVy(bouncedVy);
        }
        ball.setVx(ball.getVx() * FRICTION);
    }

    /**
     * 处理球与球之间的碰撞
     * 
     * 【算法】
     * 迭代碰撞求解器：
     * 1. 遍历所有球对（i < j，避免重复）
     * 2. 计算两球距离和重叠量
     * 3. 如果重叠且不合并：
     *    - 分离球（separateBalls）：解决位置重叠
     *    - 应用冲量（applyCollisionImpulse）：解决速度碰撞
     * 
     * 【迭代次数】
     * - SOLVER_ITERATIONS = 8
     * - 多次迭代提高碰撞求解稳定性
     * 
     * 【边界条件】
     * - 距离超过最小距离+阈值时，不触发碰撞
     * - 距离为0时（同一点），不处理（避免除零错误）
     * - 可合并的球不处理碰撞（由合并逻辑处理）
     * 
     * 【常见问题】
     * - 迭代次数不足会导致球重叠
     * - 分离逻辑不当会导致球飞起来或抖动
     */
        private void resolveBallCollisions() {
        for (int i = 0; i < balls.size(); i++) {
            Ball a = balls.get(i);
            if (!isPhysicsActive(a)) {
                continue;
            }

            for (int j = i + 1; j < balls.size(); j++) {
                Ball b = balls.get(j);
                if (!isPhysicsActive(b)) {
                    continue;
                }

                // 计算两球相对位置和距离
                double dx = b.getX() - a.getX();
                double dy = b.getY() - a.getY();
                double dist = Math.sqrt(dx * dx + dy * dy);
                double minDist = a.getRadius() + b.getRadius();

                // 距离太远或为零，不处理
                if (dist >= minDist + COLLISION_THRESHOLD || dist == 0) {
                    continue;
                }

                // 可合并的球（等级相同），跳过碰撞分离，让它们重叠以触发合并
                if (a.canMergeWith(b)) {
                    continue;
                }

                // 合并动画中的球不处理碰撞
                if (a.isMerging() || b.isMerging() || isInMergeAnimation(a) || isInMergeAnimation(b)) {
                    continue;
                }

                double aMotion = Math.hypot(a.getVx(), a.getVy());
                double bMotion = Math.hypot(b.getVx(), b.getVy());

                // 计算重叠量
                double overlap = minDist - dist;

                // 如果两球都已接近静止，且重叠极小（小于0.5像素），跳过处理以避免微抖动
                if (aMotion < 0.1 && bMotion < 0.1 && overlap < 0.5) {
                    continue;
                }
                
                // 分离球（解决位置重叠）
                separateBalls(a, b, overlap, dx, dy, dist);
                
                // 应用碰撞冲量（解决速度碰撞）
                applyCollisionImpulse(a, b, dx, dy, dist);
            }
        }
    }

    // ==================== 快速稳定 ====================
    
    /**
     * 快速吸附稳定：球速度低且接近支撑位置时，直接归零速度并吸附到支撑位置
     * 
     * 【算法】
     * 1. 遍历所有参与物理的球
     * 2. 计算球的最低支撑位置（restY）
     * 3. 如果球速度低于阈值且距离支撑位置在2像素以内：
     *    - 将垂直速度归零
     *    - 将水平速度减半（进一步阻尼）
     *    - 将球吸附到支撑位置（setY(restY)）
     * 
     * 【设计意图】
     * - 避免球在支撑位置附近微小振动导致回合无法结束
     * - 实现快速稳定，提高游戏流畅度
     * - 当球距离支撑位置足够近时，直接结束滚动
     * 
     * 【边界条件】
     * - 速度阈值：SETTLE_VELOCITY = 0.4（低于此值认为接近静止）
     * - 距离阈值：2像素（球中心与支撑位置的垂直距离）
     * - 只对物理活跃的球生效
     * 
     * 【与合成大西瓜的差异】
     * - 原版可能使用睡眠状态机制，球静止一段时间后进入睡眠状态
     * - 本实现使用直接吸附方式，更简单高效
     */
    private void snapToRest() {
        for (Ball ball : balls) {
            if (!isPhysicsActive(ball)) {
                continue;
            }

            // 计算球的速度
            double speed = Math.hypot(ball.getVx(), ball.getVy());
            
            // 如果速度高于阈值，不处理
            if (speed >= SETTLE_VELOCITY) {
                continue;
            }

            // 计算球的最低支撑位置
            double restY = findRestY(ball);
            
            // 计算球当前位置与支撑位置的距离
            double diff = ball.getY() - restY;
            
            // 如果距离在2像素以内，吸附到支撑位置
            if (Math.abs(diff) <= 2.0) {
                // 垂直速度归零
                ball.setVy(0);
                // 水平速度减半（进一步阻尼）
                ball.setVx(ball.getVx() * 0.5);
                // 吸附到支撑位置
                ball.setY(restY);
            }
        }
    }

    /**
     * 计算球的最低支撑位置（restY）
     * 
     * 【算法】
     * 1. 默认支撑位置为地面（HEIGHT - radius）
     * 2. 遍历其他所有球，计算它们能提供的支撑位置
     * 3. 返回最低的支撑位置（球能到达的最低Y坐标）
     * 
     * 【支撑来源】
     * - 地面（HEIGHT - radius）
     * - 其他球（通过圆心距计算支撑位置）
     * 
     * 【边界条件】
     * - 只考虑非删除、非合并的球作为支撑来源
     * - 水平距离超过两球半径之和+0.5时，不提供支撑
     * - 返回值不能低于地面位置
     */
    private double findRestY(Ball ball) {
        // 默认支撑位置为地面
        double restY = HEIGHT - ball.getRadius();
        
        // 遍历其他球，计算可能的支撑位置
        for (Ball other : balls) {
            if (other == ball || other.isMarkedForRemoval() || other.isMerging()) {
                continue;
            }
            
            // 计算两球水平距离
            double dx = ball.getX() - other.getX();
            double sumR = ball.getRadius() + other.getRadius();
            
            // 水平距离太远，不提供支撑
            if (Math.abs(dx) >= sumR + 0.5) {
                continue;
            }
            
            // 计算垂直方向的支撑位置
            double dy = Math.sqrt(sumR * sumR - dx * dx);
            double candidateY = other.getY() - dy;
            
            // 更新最低支撑位置
            if (candidateY < restY) {
                restY = candidateY;
            }
        }
        
        return restY;
    }

    // ==================== 合并逻辑 ====================
    
    /**
     * 处理球的合并
     * 
     * 【流程】
     * 1. 遍历所有球对（i < j）
     * 2. 计算两球距离
     * 3. 如果距离小于最小距离+阈值：
     *    - 检查是否可合并（等级相同）
     *    - 如果可合并，启动合并动画并返回（一次只合并一对）
     * 
     * 【设计意图】
     * - 一次只合并一对球，避免复杂的多球合并
     * - 合并动画期间，原球不再参与物理计算
     * 
     * 【边界条件】
     * - 只有等级相同的球才能合并
     * - 合并后的球等级+1，分数增加
     * 
     * 【常见问题】
     * - 如果合并检测过于频繁，会导致球刚接触就合并
     * - 如果合并检测延迟太大，会导致球重叠严重
     */
    private void resolveMerges() {
        for (int i = 0; i < balls.size(); i++) {
            Ball a = balls.get(i);
            if (!isPhysicsActive(a)) {
                continue;
            }

            for (int j = i + 1; j < balls.size(); j++) {
                Ball b = balls.get(j);
                if (!isPhysicsActive(b)) {
                    continue;
                }

                // 只有在稳定接触并且速度足够低时才触发合并
                if (!canMergeNow(a, b)) {
                    continue;
                }

                // 启动合并动画
                startMergeAnimation(a, b);
                return;
            }
        }
    }

    /**
     * 判断两个球当前是否可以合并
     * 
     * 【合并条件】
     * 1. 两球等级相同且低于最高等级（canMergeWith返回true）
     * 2. 两球距离在合并容忍范围内
     * 3. 两球速度都足够低（已稳定）
     * 
     * @param a 球A
     * @param b 球B
     * @return true表示可以合并，false表示不能合并
     */
    private boolean canMergeNow(Ball a, Ball b) {
        // 检查等级条件
        if (!a.canMergeWith(b)) {
            return false;
        }

        // 计算两球距离
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        double minDist = a.getRadius() + b.getRadius();

        // 检查距离是否在合并容忍范围内
        if (dist >= minDist + MERGE_DISTANCE_TOLERANCE) {
            return false;
        }

        // 检查速度是否足够低
        double aSpeed = Math.hypot(a.getVx(), a.getVy());
        double bSpeed = Math.hypot(b.getVx(), b.getVy());
        if (aSpeed >= MERGE_SPEED_LIMIT || bSpeed >= MERGE_SPEED_LIMIT) {
            return false;
        }

        return true;
    }

    /**
     * 启动合并动画
     * 
     * 【流程】
     * 1. 检查两球是否已经在合并中
     * 2. 检查两球是否已经在合并动画中
     * 3. 如果都不在，添加合并动画到列表
     * 4. 标记本帧发生了合并
     * 5. 重置静止帧数（延迟回合结束）
     * 
     * 【边界条件】
     * - 同一球不能同时参与多个合并
     * - 合并动画期间，球不参与物理计算
     */
    private void startMergeAnimation(Ball a, Ball b) {
        // 检查是否已经在合并中或合并动画中
        if (a.isMerging() || b.isMerging() || isInMergeAnimation(a) || isInMergeAnimation(b)) {
            return;
        }

        // 添加合并动画
        mergeAnimations.add(new MergeAnimation(a, b));
        
        // 播放合成音效
        playMergeSound();
        
        // 标记本帧发生了合并，延迟回合结束
        mergeOccurredThisFrame = true;
        quiescentFrames = 0;
    }

    // ==================== 碰撞处理 ====================
    
    /**
     * 分离重叠的球（位置修正）
     * 
     * 【算法】
     * 根据两球的质量和运动状态分配分离份额：
     * 1. 两球都静止：
     *    - 如果重叠很小（<1.0），用小比例分离（避免抖动）
     *    - 否则按质量比例分配：aShare = mB/(mA+mB)，bShare = mA/(mA+mB)
     * 2. 球A静止，球B运动：
     *    - 球A承担极小分离量（作为地基，大球几乎不动）
     *    - 球B承担主要分离量
     *    - 静止球份额 = MAX_STATIC_SHARE * (mA/(mA+mB))，质量越大份额越小
     * 3. 球B静止，球A运动：
     *    - 球A承担主要分离量
     *    - 球B承担极小分离量（作为地基）
     * 4. 两球都运动：
     *    - 按质量比例分配：aShare = mB/(mA+mB)，bShare = mA/(mA+mB)
     * 
     * 【质量加权原理】
     * - 大球质量大，分离时移动距离小，符合物理直觉
     * - 小球质量小，更容易被推开
     * - 静止大球几乎不动，作为稳定地基
     * - 防止新球把底层球全部撞开
     * 
     * 【设计意图】
     * - 静止的球尽量不动（作为地基）
     * - 运动的球承担主要分离量
     * - 大球比小球更难被推动
     * - 允许静止球有少量移动（实现挤压效果）
     * 
     * 【与合成大西瓜的差异】
     * - 原版可能使用更复杂的质量/惯性模型
     * - 本实现使用质量作为分离分配依据，更符合物理规律
     * 
     * 【常见问题】
     * - 如果静止球承担的分离量过大，会导致球飞起来（通过质量加权限制）
     * - 如果静止球承担的分离量过小，会导致挤压效果不明显（适当放宽MAX_STATIC_SHARE）
     * - 两球都静止时的微小重叠处理不当会导致抖动（小比例分离解决）
     */
    private void separateBalls(Ball a, Ball b, double overlap, double dx, double dy, double dist) {
        // 计算碰撞法线（从a指向b的单位向量）
        double nx = dx / dist;
        double ny = dy / dist;

        // 计算两球的运动速度
        double aMotion = Math.hypot(a.getVx(), a.getVy());
        double bMotion = Math.hypot(b.getVx(), b.getVy());
        
        // 获取两球质量
        double massA = a.getMass();
        double massB = b.getMass();
        double totalMass = massA + massB;
        
        // 根据运动状态和质量分配分离份额
        double aShare;
        double bShare;
        
        if (aMotion < SETTLE_VELOCITY && bMotion < SETTLE_VELOCITY) {
            // 两球都静止：按质量比例分配，微小重叠用小比例处理
            if (overlap < 1.0) {
                // 微小重叠：只分离一部分，让物理逐渐收敛
                aShare = 0.15;
                bShare = 0.15;
            } else {
                // 按质量比例分配：质量大的移动少
                aShare = massB / totalMass;
                bShare = massA / totalMass;
            }
        } else if (aMotion < SETTLE_VELOCITY) {
            // 球A静止：球A少动（做地基），球B多动
            // 静止球份额受质量限制，大球几乎不动
            aShare = MAX_STATIC_SHARE * (massA / totalMass);
            bShare = 1.0 - aShare;
        } else if (bMotion < SETTLE_VELOCITY) {
            // 球B静止：球A多动，球B少动
            // 静止球份额受质量限制，大球几乎不动
            bShare = MAX_STATIC_SHARE * (massB / totalMass);
            aShare = 1.0 - bShare;
        } else {
            // 两球都运动：按质量比例分配
            aShare = massB / totalMass;
            bShare = massA / totalMass;
        }

        // 根据分离份额更新位置
        a.setX(a.getX() - nx * overlap * aShare);
        a.setY(a.getY() - ny * overlap * aShare);
        b.setX(b.getX() + nx * overlap * bShare);
        b.setY(b.getY() + ny * overlap * bShare);
    }

    /**
     * 应用碰撞冲量（速度修正）
     * 
     * 【算法】
     * 基于动量守恒和弹性碰撞公式：
     * 1. 计算碰撞法线（单位向量）
     * 2. 计算相对速度在碰撞法线方向的分量（dot）
     * 3. 如果相对速度是分离的（dot <= 0），不处理
     * 4. 根据动量定理计算冲量大小：
     *    impulse = (1 + e) * dot / (1/mA + 1/mB)
     *    其中e是弹性系数，mA、mB是两球质量
     * 5. 根据冲量更新两球速度：
     *    vA' = vA - impulse * n / mA
     *    vB' = vB + impulse * n / mB
     * 
     * 【动量定理推导】
     * 动量守恒：mA*vA + mB*vB = mA*vA' + mB*vB'
     * 弹性碰撞：vB' - vA' = e * (vA - vB)
     * 解得冲量公式：impulse = (1+e) * (vA - vB)·n / (1/mA + 1/mB)
     * 
     * 【设计意图】
     * - 大球质量大，碰撞后速度变化小，符合物理直觉
     * - 小球质量小，更容易被大球弹开
     * - 动量守恒确保碰撞后系统总动量不变
     * 
     * 【边界条件】
     * - 相对速度分离时（dot <= 0），不应用冲量
     * - 反弹系数：BALL_RESTITUTION = 0.05（低反弹，模拟粘性）
     * 
     * 【常见问题】
     * - 冲量过大：球会弹开太远（通过低弹性系数控制）
     * - 冲量过小：球碰撞后没有明显反应（适当提高弹性系数）
     * - 法线计算错误：冲量方向错误，导致球异常运动
     */
    private void applyCollisionImpulse(Ball a, Ball b, double dx, double dy, double dist) {
        // 计算碰撞法线
        double nx = dx / dist;
        double ny = dy / dist;

        // 计算相对速度在法线方向的分量
        double dvx = a.getVx() - b.getVx();
        double dvy = a.getVy() - b.getVy();
        double dot = dvx * nx + dvy * ny;

        // 如果相对速度是分离的，不处理
        if (dot <= 0) {
            return;
        }

        // 获取两球质量
        double massA = a.getMass();
        double massB = b.getMass();

        // 根据动量定理计算冲量大小
        // impulse = (1 + e) * dot / (1/mA + 1/mB)
        double impulse = (1 + BALL_RESTITUTION) * dot / (1.0 / massA + 1.0 / massB);

        // 根据冲量更新速度（v' = v - impulse * n / mass）
        a.setVx(a.getVx() - impulse * nx / massA);
        a.setVy(a.getVy() - impulse * ny / massA);
        b.setVx(b.getVx() + impulse * nx / massB);
        b.setVy(b.getVy() + impulse * ny / massB);
    }

    // ==================== 清理与检测 ====================
    
    /**
     * 移除已标记为删除的球
     * 
     * 【用途】
     * - 合并后的原球被标记为删除，需要从列表中移除
     * - 使用迭代器安全移除，避免ConcurrentModificationException
     */
    private void removeMergedBalls() {
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            if (it.next().isMarkedForRemoval()) {
                it.remove();
            }
        }
    }

    /**
     * 检查是否有球越过红线（游戏结束判定）
     * 
     * 【判定条件】
     * - 球的顶部（y - radius）低于红线（RED_LINE_Y）
     * - 只要有一个球越过红线，游戏结束
     * 
     * 【边界条件】
     * - 只检查非删除、非合并的球
     * - 红线Y坐标：RED_LINE_Y = 80
     * 
     * 【常见问题】
     * - 如果红线位置过低，游戏容易结束
     * - 如果红线位置过高，游戏难度降低
     * - 球被错误地向上移动时，可能触发误判
     */
    private void checkRedLineCollision() {
        for (Ball ball : balls) {
            if (ball.isMarkedForRemoval() || ball.isMerging()) {
                continue;
            }
            // 球顶部越过红线
            if (ball.getY() - ball.getRadius() < RED_LINE_Y) {
                gameOver = true;
                break;
            }
        }
    }

    /**
     * 重新开始游戏
     * 
     * 【流程】
     * 1. 清空所有球
     * 2. 清空所有合并动画
     * 3. 重置预览球和关注球
     * 4. 重置得分和统计数据
     * 5. 重置回合状态
     * 6. 生成第一个预览球
     */
    private void restartGame() {
        balls.clear();
        mergeAnimations.clear();
        previewBall = null;
        watchedBall = null;
        score = 0;
        turnInProgress = false;
        quiescentFrames = 0;
        turnFrames = 0;
        mergeOccurredThisFrame = false;
        gameOver = false;
        maxLevelReached = 0;
        totalBallsSpawned = 0;
        totalMerges = 0;
        prepareNextPreview();
    }

    // ==================== 渲染 ====================
    
    /**
     * 渲染游戏画面
     * 
     * 【渲染顺序】
     * 1. 绘制背景
     * 2. 绘制边框
     * 3. 绘制红线
     * 4. 绘制所有球
     * 5. 绘制预览球
     * 6. 绘制得分
     * 7. 如果游戏结束，绘制游戏结束画面
     * 8. 绘制提示文字
     * 
     * 【边界条件】
     * - 只绘制非删除的球
     * - 预览球只在游戏未结束且存在时绘制
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制天蓝色渐变背景（从浅蓝到深蓝）
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235), 0, HEIGHT, new Color(70, 130, 180));
        g2d.setPaint(skyGradient);
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制白色云朵
        drawClouds(g2d);

        // 绘制边框
        g2d.setColor(new Color(60, 100, 140));
        g2d.drawRect(0, 0, WIDTH - 1, HEIGHT - 1);

        // 绘制红线
        g2d.setColor(new Color(255, 0, 0));
        g2d.setStroke(new BasicStroke(3f));
        g2d.drawLine(0, RED_LINE_Y, WIDTH, RED_LINE_Y);

        // 绘制所有球
        for (Ball ball : balls) {
            if (ball.isMarkedForRemoval()) {
                continue;
            }
            ball.draw(g2d);
        }

        // 绘制预览球
        if (!gameOver && previewBall != null) {
            previewBall.draw(g2d);
        }

        // 绘制得分
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 24));
        String scoreText = "得分: " + score;
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(scoreText, WIDTH - fm.stringWidth(scoreText) - 20, 40);

        // 绘制恭喜界面（生成终极球时显示）
        if (showCongrats) {
            drawCongratsScreen(g2d);
        }
        // 绘制游戏结束画面或提示文字
        else if (gameOver) {
            drawGameOverScreen(g2d);
        } else {
            g2d.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            g2d.setColor(new Color(200, 200, 200));
            String hint = turnInProgress ? "等待圆球落地并合成..." : "移动鼠标调整位置，点击释放圆球 | 按ESC退出";
            g2d.drawString(hint, 20, HEIGHT - 20);
        }
    }

    /**
     * 绘制恭喜界面（生成终极球时显示）
     * 
     * 【内容】
     * 1. 半透明金色背景覆盖
     * 2. "恭喜！"标题
     * 3. "成功合成终极球"提示
     * 4. 当前得分
     * 5. 烟花/星星装饰效果
     * 6. 继续游戏提示
     * 
     * 【设计意图】
     * - 庆祝玩家达成游戏最高成就（合成终极球）
     * - 金色主题，营造喜庆氛围
     * - 短暂显示后可继续游戏
     */
    private void drawCongratsScreen(Graphics2D g2d) {
        // 半透明金色背景
        g2d.setColor(new Color(255, 215, 0, 150));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 绘制装饰星星
        g2d.setColor(new Color(255, 255, 0));
        for (int i = 0; i < 20; i++) {
            int x = (int) (Math.random() * WIDTH);
            int y = (int) (Math.random() * HEIGHT);
            int size = (int) (Math.random() * 8) + 4;
            g2d.fillOval(x, y, size, size);
        }

        // 恭喜标题
        g2d.setColor(new Color(255, 165, 0));
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 56));
        String congratsText = "恭喜！";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (WIDTH - fm.stringWidth(congratsText)) / 2;
        g2d.drawString(congratsText, textX, HEIGHT / 2 - 80);

        // 成功合成提示
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 28));
        String successText = "成功合成终极球！";
        fm = g2d.getFontMetrics();
        textX = (WIDTH - fm.stringWidth(successText)) / 2;
        g2d.drawString(successText, textX, HEIGHT / 2 - 20);

        // 当前得分
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 36));
        String scoreText = "当前得分: " + score;
        fm = g2d.getFontMetrics();
        textX = (WIDTH - fm.stringWidth(scoreText)) / 2;
        g2d.drawString(scoreText, textX, HEIGHT / 2 + 40);

        // 继续游戏提示
        g2d.setColor(new Color(100, 200, 100));
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 20));
        String continueText = "点击屏幕继续游戏";
        fm = g2d.getFontMetrics();
        textX = (WIDTH - fm.stringWidth(continueText)) / 2;
        g2d.drawString(continueText, textX, HEIGHT / 2 + 100);
    }

    /**
     * 绘制白色云朵装饰背景
     * 
     * 【绘制逻辑】
     * 使用多个椭圆叠加绘制云朵形状
     * 云朵位置固定，增加画面层次感
     * 
     * 【云朵参数】
     * - 颜色：白色半透明
     * - 位置：分布在画面不同区域
     * - 大小：不同大小的云朵增加自然感
     */
    private void drawClouds(Graphics2D g2d) {
        g2d.setColor(new Color(255, 255, 255, 180));
        
        // 左上角云朵
        g2d.fillOval(20, 30, 80, 40);
        g2d.fillOval(40, 20, 60, 50);
        g2d.fillOval(70, 30, 50, 40);
        
        // 右上角云朵
        g2d.fillOval(480, 40, 90, 45);
        g2d.fillOval(500, 25, 70, 55);
        g2d.fillOval(530, 40, 60, 45);
        
        // 中间偏上云朵
        g2d.fillOval(200, 60, 100, 50);
        g2d.fillOval(220, 45, 80, 60);
        g2d.fillOval(260, 60, 70, 50);
        
        // 左下角云朵（较小）
        g2d.fillOval(30, 150, 60, 30);
        g2d.fillOval(45, 140, 45, 40);
        
        // 右下角云朵（较小）
        g2d.fillOval(490, 160, 70, 35);
        g2d.fillOval(510, 145, 50, 45);
    }

    /**
     * 绘制游戏结束画面
     * 
     * 【内容】
     * 1. 半透明黑色背景覆盖
     * 2. "游戏结束"标题
     * 3. 最终得分
     * 4. 最高等级
     * 5. 投放球数
     * 6. 合成次数
     * 7. 重新开始提示
     */
    private void drawGameOverScreen(Graphics2D g2d) {
        // 半透明背景
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 游戏结束标题
        g2d.setColor(new Color(255, 100, 100));
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 48));
        String gameOverText = "游戏结束";
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (WIDTH - fm.stringWidth(gameOverText)) / 2;
        g2d.drawString(gameOverText, textX, HEIGHT / 2 - 80);

        // 最终得分
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 36));
        String finalScore = "最终得分: " + score;
        fm = g2d.getFontMetrics();
        textX = (WIDTH - fm.stringWidth(finalScore)) / 2;
        g2d.drawString(finalScore, textX, HEIGHT / 2 - 20);

        // 最高等级
        g2d.setFont(new Font("微软雅黑", Font.PLAIN, 20));
        g2d.setColor(new Color(200, 200, 200));
        String maxLevelText = "最高等级: " + (maxLevelReached + 1);
        fm = g2d.getFontMetrics();
        textX = (WIDTH - fm.stringWidth(maxLevelText)) / 2;
        g2d.drawString(maxLevelText, textX, HEIGHT / 2 + 30);

        // 投放球数
        String totalBallsText = "投放球数: " + totalBallsSpawned;
        fm = g2d.getFontMetrics();
        textX = (WIDTH - fm.stringWidth(totalBallsText)) / 2;
        g2d.drawString(totalBallsText, textX, HEIGHT / 2 + 60);

        // 合成次数
        String totalMergesText = "合成次数: " + totalMerges;
        fm = g2d.getFontMetrics();
        textX = (WIDTH - fm.stringWidth(totalMergesText)) / 2;
        g2d.drawString(totalMergesText, textX, HEIGHT / 2 + 90);

        // 重新开始提示
        g2d.setColor(new Color(100, 200, 100));
        g2d.setFont(new Font("微软雅黑", Font.BOLD, 24));
        String restartText = "点击屏幕重新开始";
        fm = g2d.getFontMetrics();
        textX = (WIDTH - fm.stringWidth(restartText)) / 2;
        g2d.drawString(restartText, textX, HEIGHT / 2 + 150);
    }
}