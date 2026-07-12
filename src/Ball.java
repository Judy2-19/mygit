import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

/**
 * 球体类，负责存储球的物理属性和渲染
 * 
 * 【球的等级体系】
 * 等级0-6，等级越高球越大，颜色和动物图案不同
 * 
 * 【物理属性】
 * - 位置：x, y（圆心坐标）
 * - 速度：vx, vy（水平和垂直速度）
 * - 半径：根据等级确定
 * - 密度：影响碰撞时的质量分配（当前未完全使用）
 * 
 * 【状态标志】
 * - markedForRemoval：标记为待删除（合并后）
 * - merging：正在合并中（合并动画期间）
 * - mergeDrawScale：合并动画中的缩放比例
 * 
 * 【与合成大西瓜的差异】
 * - 原版球可能有更复杂的物理属性（如质量、旋转）
 * - 原版球可能有更精细的渲染效果（渐变、阴影）
 */
public class Ball {
    
    /** 各等级球的半径（像素） */
    private static final int[] RADII = {20, 40, 60, 70, 100, 120, 140};
    
    /** 各等级球的颜色 */
    private static final Color[] COLORS = {
            new Color(255, 105, 180),   // 等级0：粉色（猫）
            new Color(255, 165, 0),     // 等级1：橙色（狗）
            new Color(255, 215, 0),     // 等级2：金色（兔）
            new Color(50, 205, 50),     // 等级3：绿色（熊）
            new Color(138, 43, 226),    // 等级4：紫色（龙）
            new Color(0, 191, 255),     // 等级5：蓝色（海豚）
            new Color(255, 215, 0)      // 等级6：金色（终极球/西瓜）
    };
    
    /** 各等级球的密度（影响物理交互，当前未完全使用） */
    private static final double[] DENSITIES = {1.0, 1.0, 1.0, 1.0, 1.0, 1.2, 0.8};

    /** 球的水平位置（圆心X坐标） */
    private double x;
    /** 球的垂直位置（圆心Y坐标） */
    private double y;
    /** 球的水平速度 */
    private double vx;
    /** 球的垂直速度 */
    private double vy;
    /** 球的等级（0-6） */
    private int level;
    /** 是否标记为删除（合并后会被标记） */
    private boolean markedForRemoval;
    /** 是否正在合并中（合并动画期间） */
    private boolean merging;
    /** 合并动画中的绘制缩放比例（默认1.0） */
    private double mergeDrawScale = 1.0;

    /**
     * 构造函数：创建一个新球
     * 
     * @param x 初始X位置
     * @param y 初始Y位置
     * @param level 球的等级
     * 
     * 【初始化状态】
     * - 速度初始化为0
     * - 标记为未删除
     * - 合并状态为false
     */
    public Ball(double x, double y, int level) {
        this.x = x;
        this.y = y;
        this.level = level;
        this.vx = 0;
        this.vy = 0;
        this.markedForRemoval = false;
    }

    /**
     * 获取最高等级
     * 
     * @return 最高等级值（RADII数组长度-1）
     * 
     * 【边界条件】
     * - 最高等级为6（对应RADII数组长度7）
     */
    public static int getMaxLevel() {
        return RADII.length - 1;
    }

    /**
     * 根据等级获取球的半径
     * 
     * @param level 球的等级
     * @return 对应等级的半径
     * 
     * 【边界条件】
     * - level范围：0到getMaxLevel()
     * - 超出范围会抛出ArrayIndexOutOfBoundsException
     */
    public static int getRadiusForLevel(int level) {
        return RADII[level];
    }

    /**
     * 获取球的半径
     * 
     * @return 当前等级对应的半径
     */
    public int getRadius() {
        return RADII[level];
    }

    /**
     * 获取球的等级
     * 
     * @return 球的等级（0-6）
     */
    public int getLevel() {
        return level;
    }

    /**
     * 获取球的X位置
     * 
     * @return 圆心X坐标
     */
    public double getX() {
        return x;
    }

    /**
     * 获取球的Y位置
     * 
     * @return 圆心Y坐标
     */
    public double getY() {
        return y;
    }

    /**
     * 获取球的水平速度
     * 
     * @return 水平速度vx
     */
    public double getVx() {
        return vx;
    }

    /**
     * 获取球的垂直速度
     * 
     * @return 垂直速度vy
     */
    public double getVy() {
        return vy;
    }

    /**
     * 设置球的X位置
     * 
     * @param x 新的X坐标
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * 设置球的Y位置
     * 
     * @param y 新的Y坐标
     */
    public void setY(double y) {
        this.y = y;
    }

    /**
     * 设置球的水平速度
     * 
     * @param vx 新的水平速度
     */
    public void setVx(double vx) {
        this.vx = vx;
    }

    /**
     * 设置球的垂直速度
     * 
     * @param vy 新的垂直速度
     */
    public void setVy(double vy) {
        this.vy = vy;
    }

    /**
     * 获取球的颜色
     * 
     * @return 当前等级对应的颜色
     */
    public Color getColor() {
        return COLORS[level];
    }

    /**
     * 获取球的密度
     * 
     * @return 当前等级对应的密度
     * 
     * 【设计意图】
     * - 密度影响球的质量，用于物理碰撞计算
     * - 当前实现中密度只存储但未完全使用
     */
    public double getDensity() {
        return DENSITIES[level];
    }

    /**
     * 获取球的质量（基于体积和密度）
     * 
     * @return 球的质量
     * 
     * 【计算公式】
     * mass = π * radius² * density
     * 球体质量与半径平方成正比，密度作为修正系数
     * 
     * 【设计意图】
     * - 质量用于碰撞冲量计算，大球质量更大，更难被推动
     * - 遵循动量守恒定理：m1*v1 + m2*v2 = m1*v1' + m2*v2'
     */
    public double getMass() {
        int r = getRadius();
        return Math.PI * r * r * getDensity();
    }

    /**
     * 判断球是否被标记为删除
     * 
     * @return true表示已标记，false表示未标记
     * 
     * 【使用场景】
     * - 合并后的球会被标记为删除
     * - 下一帧会从球列表中移除
     */
    public boolean isMarkedForRemoval() {
        return markedForRemoval;
    }

    /**
     * 标记球为删除状态
     * 
     * 【使用场景】
     * - 合并动画完成后调用
     * - 标记后球不会参与物理计算
     */
    public void markForRemoval() {
        markedForRemoval = true;
    }

    /**
     * 判断球是否正在合并中
     * 
     * @return true表示正在合并，false表示未合并
     * 
     * 【使用场景】
     * - 合并动画期间，球不参与物理计算
     * - 合并完成后设置为false
     */
    public boolean isMerging() {
        return merging;
    }

    /**
     * 设置球的合并状态
     * 
     * @param merging true表示开始合并，false表示结束合并
     */
    public void setMerging(boolean merging) {
        this.merging = merging;
    }

    /**
     * 设置合并动画中的绘制缩放比例
     * 
     * @param mergeDrawScale 缩放比例（1.0为正常大小）
     * 
     * 【设计意图】
     * - 合并时球会有轻微挤压效果
     * - 通过缩放实现视觉上的挤压感
     */
    public void setMergeDrawScale(double mergeDrawScale) {
        this.mergeDrawScale = mergeDrawScale;
    }

    /**
     * 判断两个球是否可以合并
     * 
     * @param other 另一个球
     * @return true表示可以合并，false表示不能合并
     * 
     * 【合并条件】
     * 1. 两个球等级相同
     * 2. 等级低于最高等级
     * 
     * 【边界条件】
     * - 最高等级的球（等级6）不能再合并
     */
    public boolean canMergeWith(Ball other) {
        return level == other.level && level < getMaxLevel();
    }

    /**
     * 获取合并得分
     * 
     * @return 合并获得的分数
     * 
     * 【计算公式】
     * score = (level + 1) * 10
     * - 等级0合并：10分
     * - 等级1合并：20分
     * - ...
     * - 等级5合并：60分
     */
    public int getMergeScore() {
        return (level + 1) * 10;
    }

    /**
     * 判断球是否为最高等级
     * 
     * @return true表示是最高等级，false表示不是
     */
    public boolean isUltimateLevel() {
        return level == getMaxLevel();
    }

    /**
     * 绘制球
     * 
     * @param g2d Graphics2D对象
     * 
     * 【绘制逻辑】
     * 1. 根据合并缩放比例计算实际半径
     * 2. 根据等级选择不同的绘制方式：
     *    - 最高等级：绘制金色带星星的终极球
     *    - 等级5：绘制海豚
     *    - 其他等级：绘制圆形+动物图案
     */
    public void draw(Graphics2D g2d) {
        int r = (int) (getRadius() * mergeDrawScale);
        int cx = (int) x;
        int cy = (int) y;

        if (isUltimateLevel()) {
            // 最高等级：绘制金色终极球
            drawUltimateBall(g2d, cx, cy, r);
        } else if (level == 5) {
            // 等级5：绘制海豚
            drawDolphin(g2d, cx, cy, r);
        } else {
            // 其他等级：绘制圆形+动物图案
            g2d.setColor(getColor());
            g2d.fillOval(cx - r, cy - r, r * 2, r * 2);
            g2d.setColor(getColor().darker());
            g2d.drawOval(cx - r, cy - r, r * 2, r * 2);
            drawAnimal(g2d, cx, cy, r);
        }
    }

    /**
     * 根据等级绘制对应动物
     * 
     * @param g2d Graphics2D对象
     * @param cx 圆心X坐标
     * @param cy 圆心Y坐标
     * @param r 半径
     */
    private void drawAnimal(Graphics2D g2d, int cx, int cy, int r) {
        switch (level) {
            case 0:
                drawCat(g2d, cx, cy, r);
                break;
            case 1:
                drawDog(g2d, cx, cy, r);
                break;
            case 2:
                drawRabbit(g2d, cx, cy, r);
                break;
            case 3:
                drawBear(g2d, cx, cy, r);
                break;
            case 4:
                drawDragon(g2d, cx, cy, r);
                break;
            case 5:
                drawDolphin(g2d, cx, cy, r);
                break;
        }
    }

    /**
     * 绘制猫图案（等级0）
     */
    private void drawCat(Graphics2D g2d, int cx, int cy, int r) {
        // 耳朵
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(cx - r / 3, cy - r / 4, r / 4, r / 3);
        g2d.fillOval(cx + r / 12, cy - r / 4, r / 4, r / 3);
        // 耳朵内部
        g2d.setColor(new Color(255, 105, 180));
        g2d.fillOval(cx - r / 4, cy - r / 5, r / 6, r / 4);
        g2d.fillOval(cx + r / 8, cy - r / 5, r / 6, r / 4);
        // 眼睛
        g2d.setColor(Color.BLACK);
        g2d.fillOval(cx - r / 4, cy - r / 6, r / 8, r / 8);
        g2d.fillOval(cx + r / 8, cy - r / 6, r / 8, r / 8);
        // 眼睛高光
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(cx - r / 5, cy - r / 5, r / 16, r / 16);
        g2d.fillOval(cx + r / 6, cy - r / 5, r / 16, r / 16);
        // 胡须
        g2d.setColor(new Color(255, 180, 200));
        g2d.drawLine(cx - r / 3, cy + r / 6, cx - r / 2, cy + r / 4);
        g2d.drawLine(cx + r / 3, cy + r / 6, cx + r / 2, cy + r / 4);
    }

    /**
     * 绘制狗图案（等级1）
     */
    private void drawDog(Graphics2D g2d, int cx, int cy, int r) {
        // 耳朵
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(cx - r / 2, cy - r / 3, r / 4, r / 2);
        g2d.fillOval(cx + r / 4, cy - r / 3, r / 4, r / 2);
        // 耳朵内部
        g2d.setColor(new Color(210, 180, 140));
        g2d.fillOval(cx - r / 3, cy - r / 4, r / 5, r / 3);
        g2d.fillOval(cx + r / 6, cy - r / 4, r / 5, r / 3);
        // 眼睛
        g2d.setColor(Color.BLACK);
        g2d.fillOval(cx - r / 4, cy - r / 5, r / 8, r / 8);
        g2d.fillOval(cx + r / 8, cy - r / 5, r / 8, r / 8);
        // 嘴巴
        g2d.setColor(Color.BLACK);
        g2d.drawArc(cx - r / 4, cy + r / 8, r / 2, r / 4, 0, -180);
    }

    /**
     * 绘制兔子图案（等级2）
     */
    private void drawRabbit(Graphics2D g2d, int cx, int cy, int r) {
        // 耳朵
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(cx - r / 3, cy - (int)(r * 0.8), r / 5, r / 2);
        g2d.fillOval(cx + r / 6, cy - (int)(r * 0.8), r / 5, r / 2);
        // 耳朵内部
        g2d.setColor(new Color(255, 180, 200));
        g2d.fillOval(cx - r / 3, cy - (int)(r * 0.7), r / 7, r / 3);
        g2d.fillOval(cx + r / 6, cy - (int)(r * 0.7), r / 7, r / 3);
        // 眼睛
        g2d.setColor(Color.BLACK);
        g2d.fillOval(cx - r / 4, cy - r / 6, r / 8, r / 8);
        g2d.fillOval(cx + r / 8, cy - r / 6, r / 8, r / 8);
        // 腮红
        g2d.setColor(new Color(255, 180, 200));
        g2d.fillOval(cx - r / 3, cy + r / 5, r / 6, r / 8);
        g2d.fillOval(cx + r / 6, cy + r / 5, r / 6, r / 8);
    }

    /**
     * 绘制熊图案（等级3）
     */
    private void drawBear(Graphics2D g2d, int cx, int cy, int r) {
        // 耳朵
        g2d.setColor(new Color(139, 90, 43));
        g2d.fillOval(cx - r / 2, cy - r / 3, r / 3, r / 3);
        g2d.fillOval(cx + r / 6, cy - r / 3, r / 3, r / 3);
        // 耳朵内部
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(cx - r / 3, cy - r / 6, r / 6, r / 6);
        g2d.fillOval(cx + r / 6, cy - r / 6, r / 6, r / 6);
        // 眼睛
        g2d.setColor(Color.BLACK);
        g2d.fillOval(cx - r / 4, cy - r / 5, r / 8, r / 8);
        g2d.fillOval(cx + r / 8, cy - r / 5, r / 8, r / 8);
        // 眼睛高光
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(cx - r / 5, cy - r / 4, r / 16, r / 16);
        g2d.fillOval(cx + r / 6, cy - r / 4, r / 16, r / 16);
    }

    /**
     * 绘制龙图案（等级4）
     */
    private void drawDragon(Graphics2D g2d, int cx, int cy, int r) {
        // 角
        g2d.setColor(new Color(255, 0, 0));
        g2d.fillOval(cx - r / 2, cy - r / 2, r / 4, r / 2);
        g2d.fillOval(cx + r / 4, cy - r / 2, r / 4, r / 2);
        // 角装饰
        g2d.setColor(new Color(255, 200, 0));
        g2d.fillOval(cx - r / 3, cy - r / 3, r / 6, r / 3);
        g2d.fillOval(cx + r / 6, cy - r / 3, r / 6, r / 3);
        // 龙须
        g2d.setColor(new Color(255, 255, 0));
        g2d.drawLine(cx - r / 4, cy - r / 6, cx - r / 2, cy - r / 3);
        g2d.drawLine(cx + r / 8, cy - r / 6, cx + r / 2, cy - r / 3);
        // 眼睛
        g2d.setColor(Color.BLACK);
        g2d.fillOval(cx - r / 4, cy - r / 5, r / 8, r / 8);
        g2d.fillOval(cx + r / 8, cy - r / 5, r / 8, r / 8);
        // 龙鳞
        g2d.setColor(new Color(255, 200, 0));
        g2d.drawArc(cx - r / 2, cy, r, r / 2, 180, 90);
        g2d.drawArc(cx - r / 2, cy, r, r / 2, 180, -90);
    }

    /**
     * 绘制海豚图案（等级5）
     */
    private void drawDolphin(Graphics2D g2d, int cx, int cy, int r) {
        // 主体
        g2d.setColor(new Color(0, 191, 255));
        g2d.fillOval(cx - r, cy - r, r * 2, r * 2);
        g2d.setColor(new Color(0, 135, 206));
        g2d.drawOval(cx - r, cy - r, r * 2, r * 2);

        // 眼睛区域
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(cx - r / 4, cy - r / 4, r / 3, r / 3);

        // 眼睛
        g2d.setColor(Color.BLACK);
        g2d.fillOval(cx - r / 5, cy - r / 5, r / 8, r / 8);

        // 眼睛高光
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillOval(cx - r / 6, cy - r / 4, r / 16, r / 16);

        // 嘴巴
        g2d.setColor(new Color(255, 255, 255));
        g2d.fillArc(cx - r / 2, cy - r / 3, r / 2, r / 2, 180, 90);

        // 背鳍
        g2d.setColor(new Color(0, 135, 206));
        g2d.fillOval(cx + r / 2, cy - r / 4, r / 4, r / 3);
    }

    /**
     * 绘制终极球（等级6）
     */
    private void drawUltimateBall(Graphics2D g2d, int cx, int cy, int r) {
        Ellipse2D clip = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);

        // 主体颜色
        g2d.setColor(new Color(255, 215, 0));
        g2d.fill(clip);

        // 边框
        g2d.setColor(new Color(218, 165, 32));
        g2d.setStroke(new BasicStroke(Math.max(2f, r * 0.05f)));
        g2d.draw(clip);

        // 高光
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.fillOval(cx - r / 2, cy - r / 2, r, (int)(r * 0.6));

        // 星星图案
        drawStar(g2d, cx, cy, 5, r * 0.5, r * 0.25, new Color(255, 165, 0));
    }

    /**
     * 绘制星星图案
     * 
     * @param g2d Graphics2D对象
     * @param cx 中心X坐标
     * @param cy 中心Y坐标
     * @param numPoints 星角数量
     * @param outerRadius 外半径
     * @param innerRadius 内半径
     * @param color 颜色
     */
    private void drawStar(Graphics2D g2d, int cx, int cy, int numPoints, double outerRadius, double innerRadius, Color color) {
        java.awt.Polygon star = new java.awt.Polygon();
        double angleStep = Math.PI / numPoints;

        for (int i = 0; i < 2 * numPoints; i++) {
            double radius = (i % 2 == 0) ? outerRadius : innerRadius;
            double angle = i * angleStep - Math.PI / 2;
            int x = cx + (int) (Math.cos(angle) * radius);
            int y = cy + (int) (Math.sin(angle) * radius);
            star.addPoint(x, y);
        }

        // 填充星星
        g2d.setColor(color);
        g2d.fill(star);

        // 星星边框
        g2d.setColor(new Color(255, 255, 255, 150));
        g2d.setStroke(new BasicStroke(2f));
        g2d.draw(star);
    }
}