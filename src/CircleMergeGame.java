import javax.swing.JFrame;
import javax.swing.Timer;

/**
 * 游戏主入口类
 * 
 * 【功能】
 * 1. 创建游戏窗口（JFrame）
 * 2. 创建游戏面板（GamePanel）
 * 3. 设置窗口属性
 * 4. 启动游戏循环（Timer）
 * 
 * 【游戏循环】
 * - 帧率：约60fps（16ms间隔）
 * - 每帧执行：update() + repaint()
 * 
 * 【窗口属性】
 * - 标题："合成大圆球"
 * - 大小：根据GamePanel的preferredSize自动调整
 * - 位置：屏幕中央
 * - 不可调整大小
 */
public class CircleMergeGame {
    
    /**
     * 主函数：游戏入口
     * 
     * @param args 命令行参数（未使用）
     * 
     * 【执行流程】
     * 1. 创建JFrame窗口
     * 2. 创建GamePanel游戏面板
     * 3. 设置窗口关闭操作（退出程序）
     * 4. 将游戏面板添加到窗口
     * 5. 自动调整窗口大小（pack）
     * 6. 设置窗口位置为屏幕中央
     * 7. 设置窗口不可调整大小
     * 8. 显示窗口
     * 9. 创建Timer，启动游戏循环
     * 
     * 【Timer参数】
     * - delay：16ms（约60fps）
     * - ActionListener：每帧执行update()和repaint()
     */
    public static void main(String[] args) {
        // 创建窗口
        JFrame frame = new JFrame("合成大圆球");
        
        // 创建游戏面板
        GamePanel panel = new GamePanel();

        // 设置窗口属性
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // 创建并启动游戏循环
        Timer timer = new Timer(16, e -> {
            // 更新游戏逻辑
            panel.update();
            // 重绘游戏画面
            panel.repaint();
        });
        timer.start();
    }
}