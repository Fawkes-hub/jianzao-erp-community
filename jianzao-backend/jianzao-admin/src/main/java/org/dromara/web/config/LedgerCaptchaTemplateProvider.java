package org.dromara.web.config;

import cloud.tianai.captcha.resource.AbstractResourceProvider;
import cloud.tianai.captcha.resource.common.model.dto.Resource;
import org.springframework.stereotype.Component;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;

/**
 * 滑块拼图模板：运行时用 Java2D 生成与深蓝看板同色系的拼图高光与缺口阴影，
 * 避免使用官方自带的高饱和描边模板（黄/荧光绿），使验证码与登录页品牌蓝一致。
 * 透明底 PNG，零二进制图片入库。
 *
 * @author Fawkes
 */
@Component
public class LedgerCaptchaTemplateProvider extends AbstractResourceProvider {

    /** 资源类型标识，与 LedgerCaptchaConfiguration 中注册的 Resource.type 对应 */
    public static final String RESOURCE_TYPE = "ledger-template";

    /** 拼图块尺寸，与官方模板一致（110x110） */
    private static final int PIECE_SIZE = 110;

    /** 拼图块形状：主体圆角方块 + 右侧凸起圆钮，与官方 slider 模板形状对应 */
    private static final int BODY_SIZE = 70;
    private static final int BODY_OFFSET = 20;
    private static final int KNOB_SIZE = 36;
    private static final int KNOB_OFFSET_X = 74;
    private static final int KNOB_OFFSET_Y = 37;

    /** active：叠在拼图块之上的高光，半透明白让背景纹理透出来 */
    private static final Color PIECE_FILL = new Color(255, 255, 255, 58);
    private static final Color PIECE_STROKE = new Color(255, 255, 255, 235);
    /** fixed：盖在缺口处的阴影，深蓝压暗 + 浅白描边，读起来是「这里缺一块」 */
    private static final Color HOLE_FILL = new Color(8, 24, 48, 118);
    private static final Color HOLE_STROKE = new Color(255, 255, 255, 196);

    /**
     * 生成拼图模板图：按 Tianai 的取图顺序，fixed 绘制在缺口处，active 叠加在可移动拼图块上。
     *
     * @param resource 资源描述，data 指定 active / fixed
     * @return PNG 图片输入流
     */
    @Override
    public InputStream doGetResourceInputStream(Resource resource) {
        boolean active = "active".equals(resource.getData());
        BufferedImage image = new BufferedImage(PIECE_SIZE, PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Area shape = buildShape();
        g.setColor(active ? PIECE_FILL : HOLE_FILL);
        g.fill(shape);
        g.setColor(active ? PIECE_STROKE : HOLE_STROKE);
        g.setStroke(new BasicStroke(active ? 2.5f : 2f));
        g.draw(shape);
        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("生成滑块验证码模板失败", e);
        }
    }

    /**
     * 拼图块形状：圆角方块主体 + 右侧圆形凸起，中间留一个圆形凹口。
     *
     * @return 拼图块形状
     */
    private Area buildShape() {
        Area area = new Area(new RoundRectangle2D.Double(BODY_OFFSET, BODY_OFFSET, BODY_SIZE, BODY_SIZE, 10, 10));
        area.add(new Area(new Ellipse2D.Double(KNOB_OFFSET_X, KNOB_OFFSET_Y, KNOB_SIZE, KNOB_SIZE)));
        // 凹口：与凸起对称的圆洞，贴合时与凸起互补
        Path2D hole = new Path2D.Double();
        double holeSize = 18;
        double holeX = BODY_OFFSET + BODY_SIZE / 2 - holeSize / 2;
        double holeY = BODY_OFFSET + BODY_SIZE / 2 - holeSize / 2;
        hole.append(new Ellipse2D.Double(holeX, holeY, holeSize, holeSize), false);
        area.subtract(new Area(hole));
        return area;
    }

    /**
     * 仅接管 ledger-template 类型的资源。
     *
     * @param resource 资源描述
     * @return 是否为拼图模板资源
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
        return "ledger-template";
    }
}
