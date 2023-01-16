package showoff.DefaultedRenderers.Vulkan;

import static org.lwjgl.vulkan.VK10.*;

public class VulkanToolbox
{

    public static void kdvkAssertThrow(int result, int expectation)
    {
        if (result != expectation)
        {
            throw new RuntimeException(String.format("Vulkan call failed yielding: %d", result));
        }
    }

    public static void kdvkAssertThrow(int result)
    {
        kdvkAssertThrow(result, VK_SUCCESS);
    }
}
