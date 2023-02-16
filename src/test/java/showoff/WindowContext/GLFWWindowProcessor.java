package showoff.WindowContext;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkAllocationCallbacks;
import org.lwjgl.vulkan.VkInstance;

import javax.annotation.Nullable;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.*;

public class GLFWWindowProcessor implements WindowProcessor
{
    private long m_handle;
    private int m_width, m_height;
    private String m_title;
    private WndSizeCallback m_sizeCallback = null;
    private WndKeyInputCallback m_keyCallback = null;
    private WndMouseButtonCallback m_mouseButtonCallback = null;
    private WndMousePosCallback m_mousePosCallback = null;
    private WndMouseWheelCallback m_mouseWheelCallback = null;

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

        GLFWErrorCallback.createPrint(System.err).set();
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        final GLFWVidMode vidMode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        final int scaled_width = width * vidMode.width() / 1920, scaled_height = height * vidMode.height() / 1080;
        this.m_handle = glfwCreateWindow(scaled_width, scaled_height, this.m_title, MemoryUtil.NULL, MemoryUtil.NULL);
        if (this.m_handle == MemoryUtil.NULL) return false;

        glfwSetFramebufferSizeCallback(this.m_handle, (__, w, h) ->
        {
            this.m_width = w;
            this.m_height = h;
            if (this.m_sizeCallback != null) this.m_sizeCallback.invoke(this.m_width, this.m_height);
        });
        glfwSetCursorPosCallback(this.m_handle, (__, x, y) ->
        {
            if (this.m_mousePosCallback != null) this.m_mousePosCallback.invoke(x, y);
        });
        glfwSetMouseButtonCallback(this.m_handle, (__, button, action, mods) ->
        {
            if (this.m_mouseButtonCallback != null) this.m_mouseButtonCallback.invoke(button, action == GLFW_PRESS ? MouseButtonAction.PRESS : MouseButtonAction.RELEASE, mods);
        });
        glfwSetKeyCallback(this.m_handle, (__, key, scancode, action, mods) ->
        {
            if (this.m_keyCallback != null) this.m_keyCallback.invoke(key, scancode, action, mods);
        });
        glfwSetScrollCallback(this.m_handle, (__, xo, yo) ->
        {
            if (this.m_mouseWheelCallback != null) this.m_mouseWheelCallback.invoke(xo, yo);
        });

        this.m_width = width;
        this.m_height = height;
        glfwSetWindowPos(this.m_handle, (vidMode.width() - scaled_width) / 2, (vidMode.height() - scaled_height) / 2);
        glfwShowWindow(this.m_handle);
        return true;
    }

    @Override
    public void endWindowContext()
    {
        glfwDestroyWindow(this.m_handle);
        this.m_handle = MemoryUtil.NULL;
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

    @Override public void setWndSizeCallback(@Nullable WndSizeCallback callback)
    {
        this.m_sizeCallback = callback;
    }
    @Override public void setWndMousePosCallback(@Nullable WndMousePosCallback callback)
    {
        this.m_mousePosCallback = callback;
    }
    @Override public void setWndMouseButtonCallback(@Nullable WndMouseButtonCallback callback)
    {
        this.m_mouseButtonCallback = callback;
    }
    @Override public void setWndMouseWheelCallback(@Nullable WndMouseWheelCallback callback)
    {
        this.m_mouseWheelCallback = callback;
    }
    @Override public void setWndKeyInputCallback(@Nullable WndKeyInputCallback callback)
    {
        this.m_keyCallback = callback;
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
