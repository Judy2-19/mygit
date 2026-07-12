/**
 * 合并动画类，负责处理两个球合并的动画效果
 * 
 * 【动画流程】
 * 1. 记录两个球的初始位置
 * 2. 计算合并目标位置（两球中点）
 * 3. 每帧更新球的位置，向目标位置移动
 * 4. 添加缩放效果（模拟挤压）
 * 5. 动画完成后创建结果球
 * 6. 标记原球为删除
 * 
 * 【动画参数】
 * - TOTAL_FRAMES：总帧数（18帧，约0.3秒）
 * - easeInOut：缓动函数（先加速后减速）
 * - squash：缩放系数（1.0 + 0.08 * sin(π*t)）
 * 
 * 【与合成大西瓜的差异】
 * - 原版可能有更复杂的合并效果（粒子、光效）
 * - 原版可能有音效伴随合并
 */
public class MergeAnimation {
    
    /** 合并动画总帧数（约0.05秒，60fps） */
    private static final int TOTAL_FRAMES = 3;

    /** 参与合并的球A */
    private final Ball ballA;
    /** 参与合并的球B */
    private final Ball ballB;
    
    /** 球A的初始X位置 */
    private final double startAX;
    /** 球A的初始Y位置 */
    private final double startAY;
    /** 球B的初始X位置 */
    private final double startBX;
    /** 球B的初始Y位置 */
    private final double startBY;
    
    /** 合并目标位置X（两球中点） */
    private final double targetX;
    /** 合并目标位置Y（两球中点） */
    private final double targetY;
    /** 合并结果球的等级（原等级+1） */
    private final int resultLevel;
    
    /** 当前动画帧数 */
    private int frame;

    /**
     * 构造函数：创建合并动画
     * 
     * @param a 参与合并的第一个球
     * @param b 参与合并的第二个球
     * 
     * 【初始化流程】
     * 1. 保存两个球的引用
     * 2. 记录初始位置
     * 3. 计算目标位置（两球中点）
     * 4. 计算结果球等级
     * 5. 设置两球的合并状态为true
     * 
     * 【边界条件】
     * - 两球必须等级相同且低于最高等级
     * - 合并状态设置后，球不再参与物理计算
     */
    public MergeAnimation(Ball a, Ball b) {
        ballA = a;
        ballB = b;
        
        // 记录初始位置
        startAX = a.getX();
        startAY = a.getY();
        startBX = b.getX();
        startBY = b.getY();
        
        // 计算目标位置（两球中点）
        targetX = (startAX + startBX) / 2;
        targetY = (startAY + startBY) / 2;
        
        // 计算结果球等级
        resultLevel = a.getLevel() + 1;
        
        // 设置合并状态，防止参与物理计算
        a.setMerging(true);
        b.setMerging(true);
    }

    /**
     * 更新动画帧
     * 
     * @return true表示动画已完成，false表示还在进行中
     * 
     * 【更新流程】
     * 1. 帧数+1
     * 2. 计算进度t（0到1）
     * 3. 应用缓动函数
     * 4. 更新两球位置（向目标位置移动）
     * 5. 更新缩放效果（模拟挤压）
     * 6. 判断是否完成
     * 
     * 【缓动函数】
     * easeInOut：先加速后减速，使动画更自然
     * 
     * 【缩放效果】
     * squash = 1.0 + 0.08 * sin(π*t)
     * - 前半段逐渐变大（挤压）
     * - 后半段逐渐恢复（释放）
     */
    public boolean update() {
        // 帧数+1
        frame++;
        
        // 计算进度（0到1）
        double t = Math.min(1.0, frame / (double) TOTAL_FRAMES);
        
        // 应用缓动函数
        double eased = easeInOut(t);

        // 更新球A位置
        ballA.setX(startAX + (targetX - startAX) * eased);
        ballA.setY(startAY + (targetY - startAY) * eased);
        
        // 更新球B位置
        ballB.setX(startBX + (targetX - startBX) * eased);
        ballB.setY(startBY + (targetY - startBY) * eased);

        // 更新缩放效果（模拟挤压），幅度更大但持续更短
        double squash = 1.0 + 0.16 * Math.sin(t * Math.PI);
        ballA.setMergeDrawScale(squash);
        ballB.setMergeDrawScale(squash);

        // 判断是否完成
        return frame >= TOTAL_FRAMES;
    }

    /**
     * 创建合并结果球
     * 
     * @return 合并后的新球
     * 
     * 【创建流程】
     * 1. 计算结果球位置（两球当前位置中点）
     * 2. 创建新球（等级为原等级+1）
     * 3. 设置速度为0（静止状态）
     * 
     * 【边界条件】
     * - 结果球等级不能超过最高等级
     * - 结果球初始速度为0，避免突然运动
     */
    public Ball createResultBall() {
        double x = (ballA.getX() + ballB.getX()) / 2;
        double y = (ballA.getY() + ballB.getY()) / 2;
        Ball merged = new Ball(x, y, resultLevel);
        
        // 设置速度为0，避免合并后球突然运动
        merged.setVy(0);
        merged.setVx(0);
        
        return merged;
    }

    /**
     * 完成合并动画
     * 
     * 【流程】
     * 1. 标记两个原球为删除
     * 2. 重置合并状态为false
     * 3. 重置缩放比例为1.0
     * 
     * 【使用场景】
     * - 动画完成后调用
     * - 原球会在下一帧被移除
     */
    public void finish() {
        // 标记原球为删除
        ballA.markForRemoval();
        ballB.markForRemoval();
        
        // 重置合并状态
        ballA.setMerging(false);
        ballB.setMerging(false);
        
        // 重置缩放比例
        ballA.setMergeDrawScale(1.0);
        ballB.setMergeDrawScale(1.0);
    }

    /**
     * 判断某个球是否参与了这个合并动画
     * 
     * @param ball 要检查的球
     * @return true表示参与了，false表示没参与
     * 
     * 【使用场景】
     * - 更新关注球时检查
     * - 判断球是否在合并动画中
     */
    public boolean involves(Ball ball) {
        return ball == ballA || ball == ballB;
    }

    /**
     * 缓动函数（easeInOut）
     * 
     * @param t 进度（0到1）
     * @return 缓动后的进度
     * 
     * 【公式】
     * t < 0.5: 2*t*t（加速）
     * t >= 0.5: 1 - (-2*t + 2)^2 / 2（减速）
     * 
     * 【效果】
     * - 开始时缓慢，中间加速，结束时缓慢
     * - 使动画更自然流畅
     */
    private static double easeInOut(double t) {
        return 1 - Math.pow(1 - t, 3);
    }
}