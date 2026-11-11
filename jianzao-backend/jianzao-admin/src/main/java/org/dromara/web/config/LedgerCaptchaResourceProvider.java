package org.dromara.web.config;

import cloud.tianai.captcha.resource.AbstractResourceProvider;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import org.springframework.stereotype.Component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.ImageIO;

/**
 * 滑块验证码背景图提供者：服务机加离散制造行业，提供多套工业场景背景图：
 * 1. 车间 MES 智能看板（精密齿轮 + 产线走势折线 + OEE 指标）
 * 2. 机加生产流转卡（工艺路线 OP10~OP40 + 40Cr 材质 + 检验印章）
 * 3. 零件工程 CAD 蓝图（剖面几何 + 公差尺寸标注 Ø38/Ra1.6 + 标题栏）
 * 4. CNC 刀具切削参数（立铣刀刃口剪影 + Vc/fz 切削速度 + G54 工件坐标）
 * 运行时由 Java2D 纯代码矢量绘制，免外部图片依赖，使用 ASCII 工程术语避开容器中文字体缺失。
 *
 * @author Fawkes
 */
@Component
public class LedgerCaptchaResourceProvider extends AbstractResourceProvider {

    /** 资源类型标识，与 LedgerCaptchaConfiguration 中注册的 Resource.type 对应 */
    public static final String RESOURCE_TYPE = "ledger";

    private static final int WIDTH = 600;
    private static final int HEIGHT = 360;

    /**
     * 根据资源标识或随机种子渲染对应的机加行业背景图。
     *
     * @param resource 资源描述
     * @return JPEG 图片输入流
     */
    @Override
    public InputStream doGetResourceInputStream(Resource resource) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int seed = ThreadLocalRandom.current().nextInt(1000);
        String tag = resource != null && resource.getData() != null ? resource.getData().toString() : "";
        if ("machining-kanban".equals(tag)) {
            drawMachiningKanban(g, seed);
        } else if ("machining-traveler".equals(tag)) {
            drawMachiningTraveler(g, seed);
        } else if ("machining-blueprint".equals(tag)) {
            drawMachiningBlueprint(g, seed);
        } else if ("machining-cnc-tool".equals(tag)) {
            drawMachiningCncTool(g, seed);
        } else {
            // 兜底按种子随机分发
            int pick = seed % 4;
            if (pick == 0) {
                drawMachiningKanban(g, seed);
            } else if (pick == 1) {
                drawMachiningTraveler(g, seed);
            } else if (pick == 2) {
                drawMachiningBlueprint(g, seed);
            } else {
                drawMachiningCncTool(g, seed);
            }
        }

        g.dispose();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "jpg", out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("绘制机加滑块验证码背景失败", e);
        }
    }

    /**
     * 样式一：车间 MES 看板（齿轮 + 产线折线走势 + 实时转速与 OEE 指标卡）。
     *
     * @param g    画布
     * @param seed 随机因子
     */
    private void drawMachiningKanban(Graphics2D g, int seed) {
        g.setPaint(new GradientPaint(0, 0, new Color(0x2f6fde), WIDTH, HEIGHT, new Color(0x0a2547)));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 蓝图细网格
        g.setColor(new Color(255, 255, 255, 22));
        g.setStroke(new BasicStroke(1f));
        for (int x = 30; x < WIDTH; x += 30) g.drawLine(x, 0, x, HEIGHT);
        for (int y = 30; y < HEIGHT; y += 30) g.drawLine(0, y, WIDTH, y);

        // 双齿轮啮合剪影
        drawGear(g, 470, 180, 80, 8);
        drawGear(g, 370, 240, 50, 6);

        // 产能与切削负荷折线
        g.setColor(new Color(0x78fbbb));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D trend = new Path2D.Double();
        int yOff = (seed % 15) - 7;
        trend.moveTo(36, 250 + yOff);
        trend.lineTo(120, 200 - yOff);
        trend.lineTo(198, 228 + yOff);
        trend.lineTo(292, 146);
        trend.lineTo(378, 176 - yOff);
        trend.lineTo(498, 108);
        g.draw(trend);

        // 胶囊标签与机床编号
        g.setColor(new Color(255, 255, 255, 32));
        g.fill(new RoundRectangle2D.Double(24, 22, 160, 30, 15, 15));
        g.setColor(new Color(0x78fbbb));
        g.fill(new Ellipse2D.Double(38, 33, 8, 8));
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("CNC-0" + (seed % 8 + 1) + " RUNNING", 54, 43);

        // 底部 OEE 指标
        g.setColor(new Color(255, 255, 255, 24));
        g.fill(new RoundRectangle2D.Double(24, 280, 190, 56, 10, 10));
        g.setColor(new Color(255, 255, 255, 160));
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("OEE: 89." + (seed % 9) + "%  RPM: " + (9000 + (seed % 15) * 200), 36, 304);
        g.drawString("FEED: F1200 mm/min", 36, 324);
        g.drawString("JIANZAO ERP - MES", 440, 340);
    }

    /**
     * 样式二：机加生产流转卡（浅色工程单据底 + 40Cr 材质规格 + 工步工序表格 + 质检水印）。
     *
     * @param g    画布
     * @param seed 随机因子
     */
    private void drawMachiningTraveler(Graphics2D g, int seed) {
        // 浅灰高质感工程纸底色
        g.setColor(new Color(0xf4f6fb));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 方格装配底纹
        g.setColor(new Color(0xe2e6f0));
        g.setStroke(new BasicStroke(0.8f));
        for (int x = 25; x < WIDTH; x += 25) g.drawLine(x, 0, x, HEIGHT);
        for (int y = 25; y < HEIGHT; y += 25) g.drawLine(0, y, WIDTH, y);

        // 单据主卡片外框
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(20, 16, 560, 328, 12, 12));
        g.setColor(new Color(0xc5c9d6));
        g.setStroke(new BasicStroke(1.2f));
        g.draw(new RoundRectangle2D.Double(20, 16, 560, 328, 12, 12));

        // 表头
        g.setColor(new Color(0x0052d9));
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString("JOB TRAVELER / 工艺流转卡", 40, 48);

        g.setColor(new Color(0x606573));
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("WO: WO-20261108-" + (1000 + seed % 9000), 40, 72);
        g.drawString("PART: SHAFT-40Cr  SPEC: D48x160mm  QTY: " + (50 + seed % 200) + " PCS", 40, 92);

        // 表格横线与工步内容
        g.setColor(new Color(0xd8dbe5));
        g.drawLine(40, 106, 540, 106);
        String[] ops = {
            "OP 10  CNC LATHE  粗车外圆及端面      TOL: +-0.05    OK",
            "OP 20  4-AXIS MILL 铣削键槽与分度孔    SPEED: S2400   OK",
            "OP 30  HEAT TREAT 高频淬火强化处理    HARD: HRC48-52 OK",
            "OP 40  CNC GRIND   外圆精密磨削        ROUGH: Ra0.8   OK"
        };
        int y = 132;
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        for (String op : ops) {
            g.setColor(new Color(0x404553));
            g.drawString(op, 44, y);
            g.setColor(new Color(0xeef0f5));
            g.drawLine(40, y + 8, 540, y + 8);
            y += 32;
        }

        // 右下角质检印章水印
        g.setColor(new Color(0x00a870, false));
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(410, 240, 120, 52, 10, 10));
        g.setFont(new Font("SansSerif", Font.BOLD, 14));
        g.drawString("QC PASSED", 428, 264);
        g.setFont(new Font("Monospaced", Font.PLAIN, 10));
        g.drawString("INSPECTOR 0" + (seed % 5 + 1), 428, 280);
    }

    /**
     * 样式三：机械零件 CAD 蓝图（工程深蓝图纸 + 同心圆法兰/阶梯轴剖面 + 公差倒角尺寸标注）。
     *
     * @param g    画布
     * @param seed 随机因子
     */
    private void drawMachiningBlueprint(Graphics2D g, int seed) {
        // 经典蓝图渐变深蓝
        g.setPaint(new GradientPaint(0, 0, new Color(0x0b2d64), WIDTH, HEIGHT, new Color(0x051634)));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 蓝图密集网格（20px）
        g.setColor(new Color(255, 255, 255, 18));
        g.setStroke(new BasicStroke(0.7f));
        for (int x = 20; x < WIDTH; x += 20) g.drawLine(x, 0, x, HEIGHT);
        for (int y = 20; y < HEIGHT; y += 20) g.drawLine(0, y, WIDTH, y);

        // 中心法兰盘剖面几何图形
        int cx = 220;
        int cy = 180;
        g.setColor(new Color(255, 255, 255, 200));
        g.setStroke(new BasicStroke(1.8f));
        g.draw(new Ellipse2D.Double(cx - 90, cy - 90, 180, 180));
        g.draw(new Ellipse2D.Double(cx - 55, cy - 55, 110, 110));
        g.draw(new Ellipse2D.Double(cx - 25, cy - 25, 50, 50));

        // 4 颗螺栓分度圆孔
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 2 * i;
            double hx = cx + Math.cos(angle) * 72;
            double hy = cy + Math.sin(angle) * 72;
            g.draw(new Ellipse2D.Double(hx - 8, hy - 8, 16, 16));
        }

        // 中心对称红/淡蓝点划线
        g.setColor(new Color(0x78fbbb));
        float[] dash = {10.0f, 4.0f, 2.0f, 4.0f};
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
        g.drawLine(cx - 120, cy, cx + 120, cy);
        g.drawLine(cx, cy - 120, cx, cy + 120);

        // CAD 尺寸标注线
        g.setColor(new Color(255, 255, 255, 180));
        g.setStroke(new BasicStroke(1f));
        g.drawLine(cx + 90, cy - 90, 420, cy - 90);
        g.drawLine(cx + 90, cy + 90, 420, cy + 90);
        g.drawLine(410, cy - 90, 410, cy + 90);
        g.setFont(new Font("Monospaced", Font.BOLD, 12));
        g.drawString("D180 h7 (+0/-0.02)", 420, cy + 4);
        g.drawString("Ra 1.6 / C2.0", 380, cy - 40);

        // 蓝图右下标题栏
        g.setColor(new Color(255, 255, 255, 36));
        g.fill(new RoundRectangle2D.Double(360, 270, 216, 68, 6, 6));
        g.setColor(new Color(255, 255, 255, 180));
        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g.drawString("DWG: JZ-M08-FLANGE", 372, 292);
        g.drawString("MATERIAL: 40Cr HRC45-50", 372, 310);
        g.drawString("TOL: ISO 2768-m  SCALE 1:1", 372, 326);
    }

    /**
     * 样式四：CNC 数控刀具与切削加工参数（立铣刀几何外形 + 切削转速进给 + G54 工件坐标卡）。
     *
     * @param g    画布
     * @param seed 随机因子
     */
    private void drawMachiningCncTool(Graphics2D g, int seed) {
        // 暗夜钛金/碳钢冷调深色底
        g.setPaint(new GradientPaint(0, 0, new Color(0x19212d), WIDTH, HEIGHT, new Color(0x0e131b)));
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // 工业坐标微光点阵
        g.setColor(new Color(255, 255, 255, 18));
        for (int x = 20; x < WIDTH; x += 40) {
            for (int y = 20; y < HEIGHT; y += 40) {
                g.fillRect(x, y, 2, 2);
            }
        }

        // 刀具外轮廓（立铣刀 BT40 刀柄与 4 刃立铣刀几何）
        g.setColor(new Color(255, 255, 255, 45));
        g.fill(new RoundRectangle2D.Double(80, 40, 80, 110, 8, 8)); // 刀柄锥体
        g.setColor(new Color(0x2f6fde));
        g.fill(new RoundRectangle2D.Double(95, 150, 50, 140, 4, 4)); // 铣刀刀体
        // 螺旋排屑槽线条
        g.setColor(new Color(255, 255, 255, 100));
        g.setStroke(new BasicStroke(2f));
        g.drawLine(95, 170, 145, 195);
        g.drawLine(95, 210, 145, 235);
        g.drawLine(95, 250, 145, 275);

        // 切削速度参数仪表卡
        g.setColor(new Color(255, 255, 255, 22));
        g.fill(new RoundRectangle2D.Double(220, 40, 340, 130, 12, 12));
        g.setColor(new Color(0x78fbbb));
        g.setFont(new Font("Monospaced", Font.BOLD, 14));
        g.drawString("CUTTING CONDITIONS / 切削参数", 240, 68);
        g.setColor(new Color(255, 255, 255, 210));
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("Vc : " + (180 + seed % 60) + " m/min    fz: 0.08 mm/z", 240, 96);
        g.drawString("RPM: " + (6000 + (seed % 10) * 300) + " rev/min  ap: 3.5mm  ae: 12mm", 240, 120);
        g.drawString("TOOL: 4-FLUTE SOLID CARBIDE D12 (硬质合金)", 240, 144);

        // G54 工件坐标系卡片
        g.setColor(new Color(255, 255, 255, 18));
        g.fill(new RoundRectangle2D.Double(220, 190, 340, 120, 12, 12));
        g.setColor(new Color(0x2f6fde));
        g.setFont(new Font("Monospaced", Font.BOLD, 13));
        g.drawString("WORK COORDINATE [G54]", 240, 218);
        g.setColor(new Color(255, 255, 255, 180));
        g.setFont(new Font("Monospaced", Font.PLAIN, 12));
        g.drawString("X: +182." + (100 + seed % 800) + " mm", 240, 246);
        g.drawString("Y: -96." + (100 + seed % 800) + " mm", 240, 270);
        g.drawString("Z: -18.500 mm   B: 0.000 deg", 240, 294);
    }

    /**
     * 绘制齿轮剪影：外环与轮齿。
     *
     * @param g       画布
     * @param centerX 圆心 X
     * @param centerY 圆心 Y
     * @param radius  半径
     * @param teeth   齿数
     */
    private void drawGear(Graphics2D g, int centerX, int centerY, int radius, int teeth) {
        g.setColor(new Color(255, 255, 255, 30));
        g.setStroke(new BasicStroke(14f));
        g.draw(new Ellipse2D.Double(centerX - radius, centerY - radius, radius * 2.0, radius * 2.0));
        int toothR = 12;
        for (int i = 0; i < teeth; i++) {
            double angle = (2 * Math.PI / teeth) * i;
            double tx = centerX + Math.cos(angle) * radius;
            double ty = centerY + Math.sin(angle) * radius;
            g.fill(new Ellipse2D.Double(tx - toothR, ty - toothR, toothR * 2.0, toothR * 2.0));
        }
    }

    /**
     * 仅接管 ledger 类型的资源。
     *
     * @param resource 资源描述
     * @return 是否支持
     */
    @Override
    public boolean supported(Resource resource) {
        return RESOURCE_TYPE.equals(resource.getType());
    }

    /**
     * @return provider 名称
     */
    @Override
    public String getName() {
        return "machining-multistyle";
    }
}
