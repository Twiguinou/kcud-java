package showoff.WindowContext;

import org.lwjgl.vulkan.KHRSurface;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.LongBuffer;

public class AWTWindowProcessor implements WindowProcessor
{
    private final JFrame m_jFrame = new JFrame();
    private final Canvas m_canvas = new Canvas();
    private boolean m_closing = false;

    public AWTWindowProcessor()
    {
        this.m_jFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.m_jFrame.addWindowListener(new WindowAdapter()
        {
            @Override public void windowClosing(WindowEvent e)
            {
                super.windowClosing(e);
                AWTWindowProcessor.this.m_closing = true;
            }
        });
        this.m_jFrame.setLayout(new BorderLayout());
        this.m_jFrame.add(this.m_canvas);
    }

    @Override
    public boolean createWindowContext(int width, int height)
    {
        this.m_jFrame.setPreferredSize(new Dimension(width, height));
        this.m_jFrame.pack();
        this.m_jFrame.setVisible(true);
        return true;
    }

    @Override
    public void endWindowContext()
    {
        this.m_jFrame.setVisible(false);
        this.m_jFrame.dispose();
    }

    @Override
    public int getWidth()
    {
        return this.m_jFrame.getWidth();
    }

    @Override
    public int getHeight()
    {
        return this.m_jFrame.getHeight();
    }

    @Override
    public String getWindowTitle()
    {
        return this.m_jFrame.getTitle();
    }

    @Override
    public void setWindowTitle(String title)
    {
        this.m_jFrame.setTitle(title);
    }

    @Override
    public void beginRenderStage() {}

    @Override
    public void endRenderStage() {}

    @Override
    public boolean windowShouldClose()
    {
        return this.m_closing;
    }

    @Override
    public String[] getVulkanExtensions()
    {
        return new String[] {KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME, AWTHelper.getSurfaceExtension()};
    }

    @Override
    public int createVulkanSurface(VkInstance instance, VkAllocationCallbacks allocator, LongBuffer dest)
    {
        return AWTHelper.VkSurface(instance, allocator, this.m_canvas, dest);
    }
}
