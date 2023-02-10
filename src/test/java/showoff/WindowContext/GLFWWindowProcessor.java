package showoff.WindowContext;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;

import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;

public class GLFWWindowProcessor implements WindowProcessor
{

    private long m_handle;
    private int m_width, m_height;
    private String m_title;

    public GLFWWindowProcessor()
    {
        this.m_handle = MemoryUtil.NULL;
        this.m_width = 0;
        this.m_height = 0;
        this.m_title = "";
    }

    @Override
    public boolean createWindowContext(int width, int height)
    {
        if (!glfwInit()) return false;
        endWindowContext();

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        final GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        final int scaled_width = width * vidMode.width() / 1920, scaled_height = height * vidMode.height() / 1080;
        this.m_handle = glfwCreateWindow(scaled_width, scaled_height, this.m_title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (this.m_handle == MemoryUtil.NULL) return false;

        this.m_width = width;
        this.m_height = height;
        glfwSetWindowPos(this.m_handle, (vidMode.width() - scaled_width) / 2, (vidMode.height() - scaled_height) / 2);
        glfwShowWindow(this.m_handle);
        return true;
    }

    @Override
    public void endWindowContext()
    {
        if (this.m_handle != MemoryUtil.NULL)
        {
            glfwDestroyWindow(this.m_handle);
            this.m_handle = MemoryUtil.NULL;
        }
    }

    @Override public int getWidth() {return this.m_width;}
    @Override public int getHeight() {return this.m_height;}
    @Override public String getWindowTitle() {return this.m_title;}
    @Override public void setWindowTitle(String title) {this.m_title = title;}

    @Override
    public void beginRenderStage()
    {
        glfwPollEvents();
    }

    @Override
    public void endRenderStage() {}

    @Override
    public boolean windowShouldClose()
    {
        return glfwWindowShouldClose(this.m_handle);
    }

    @Override
    public String[] getVulkanExtensions()
    {
        PointerBuffer glfw_extensions = glfwGetRequiredInstanceExtensions();
        String[] result = new String[glfw_extensions.capacity()];
        for (int i = 0; i < glfw_extensions.capacity(); i++) result[i] = glfw_extensions.getStringUTF8(i);
        return result;
    }

    @Override
    public int createVulkanSurface(VkInstance instance, VkAllocationCallbacks allocator, LongBuffer dest)
    {
        return glfwCreateWindowSurface(instance, this.m_handle, allocator, dest);
    }
}
