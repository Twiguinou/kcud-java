package showoff.DefaultedRenderers.Vulkan;

import static org.lwjgl.vulkan.VK12.VK_SUCCESS;

public class VulkanException extends RuntimeException
{
    public VulkanException(String s)
    {
        super(s);
    }

    public VulkanException(String s, int vk_error)
    {
        super(String.format("Vulkan fail: %d | %s", vk_error, s));
    }

    public static void check(int vk_error) throws VulkanException
    {
        if (vk_error != VK_SUCCESS) throw new VulkanException("Vulkan fail: " + vk_error);
    }

    public static void check(int vk_error, String s) throws VulkanException
    {
        if (vk_error != VK_SUCCESS) throw new VulkanException(s, vk_error);
    }
}
