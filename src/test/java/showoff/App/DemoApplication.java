package showoff.App;

import kcud.ContraptionNalgebra.kdVector3;
import showoff.App.Render.Camera;
import showoff.App.Render.VulkanRenderer;
import showoff.WindowContext.WindowProcessor;

import java.awt.event.KeyEvent;

import static kcud.ContraptionNalgebra.kdMathDefs.*;

public class DemoApplication
{
    private final WindowProcessor m_windowProcessor;
    private final VulkanRenderer m_renderer;
    private final Camera m_camera;
    private final WindowInputMap m_inputs = new WindowInputMap();
    private boolean m_running = false;

    public DemoApplication(WindowProcessor windowProc, int width, int height, boolean debug)
    {
        this.m_windowProcessor = windowProc;
        this.m_windowProcessor.setWindowTitle("showoff");
        if (!this.m_windowProcessor.createWindowContext(width, height))
        {
            throw new RuntimeException("Window context initialization failed.");
        }

        this.m_renderer = new VulkanRenderer("showoff", this.m_windowProcessor, debug);
        this.m_camera = new Camera(new kdVector3(), new kdVector3(5.f, 3.f, 2.f));
        this.m_camera.setProjection(kdRadians(75.f), (float)windowProc.getWidth() / windowProc.getHeight(), 0.1f, 1000.f, true);
        this.m_inputs.registerCallbacks(windowProc, (xm, ym) -> this.m_camera.addDistanceOffset((float)-ym), (xo, yo) ->
        {
            float scaledx = xo.floatValue() * 0.001f, scaledy = yo.floatValue() * 0.001f;
            if (this.m_inputs.getMouse(0) == WindowProcessor.MouseButtonAction.PRESS)
            {
                this.m_camera.rotateView(scaledx * 5.f, scaledy * -5.f);
            }
            else if (this.m_inputs.getMouse(1) == WindowProcessor.MouseButtonAction.PRESS)
            {
                this.m_camera.moveTarget(scaledx, scaledy);
            }
            else if (this.m_inputs.getMouse(2) == WindowProcessor.MouseButtonAction.PRESS)
            {
                this.m_camera.rotateTarget(scaledx * 2.f, scaledy * 2.f);
            }
        });
    }

    public void setupDemo(DemoInterface demo)
    {
        this.m_camera.reset();
        demo.setupScene(this.m_camera);
    }

    private void updateInputs()
    {
        if (this.m_inputs.get(KeyEvent.VK_ESCAPE) == WindowProcessor.KeyInputState.PRESS) this.m_running = false;
        if (this.m_inputs.get(KeyEvent.VK_C) == WindowProcessor.KeyInputState.PRESS) this.m_camera.reset();
        this.m_camera.updateViewMatrix();
    }

    public void run()
    {
        this.m_running = true;
        while (this.m_running && !this.m_windowProcessor.windowShouldClose())
        {
            this.m_windowProcessor.beginRenderStage();
            this.updateInputs();

            this.m_renderer.renderFrame(this.m_camera);

            this.m_windowProcessor.endRenderStage();
        }

        this.destroy();
    }

    public void destroy()
    {
        this.m_renderer.destroy();
        this.m_windowProcessor.dispose();
    }
}
