package showoff.DefaultedRenderers.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import showoff.ForeignStack;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static showoff.DefaultedRenderers.Vulkan.VulkanToolbox.kdvkAssertThrow;

public class LogicalDevice
{
    public record QueueRequirements(
            int capacities,
            long[] pSurfaceSupport,
            QueueRequirements next,
            boolean strict_existence
    ) {}

    public record QueueProperties(
            VkQueue handle,
            int family,
            int index
    ) {}

    private VkDevice m_internalHandle;
    private final List<String> m_enabledExtensions;
    public final List<QueueProperties> m_generatedQueues;

    public LogicalDevice()
    {
        this.m_internalHandle = null;
        this.m_enabledExtensions = new ArrayList<>();
        this.m_generatedQueues = new ArrayList<>();
    }

    private static Stream<String> filterDeviceExtensions(VkPhysicalDevice physicalDevice, Stream<String> targetExtensions)
    {
        try (ForeignStack stack = ForeignStack.pushConfined())
        {
            IntBuffer extPropertiesListCount = stack.mallocInt(1);
            kdvkAssertThrow(vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer)null, extPropertiesListCount, null));
            VkExtensionProperties.Buffer extensionPropertiesList = VkExtensionProperties.malloc(extPropertiesListCount.get(0), stack);
            kdvkAssertThrow(vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer)null, extPropertiesListCount, extensionPropertiesList));
            return targetExtensions.filter(ext ->
            {
                for (int j = 0; j < extPropertiesListCount.get(0); j++)
                {
                    if (ext.equals(extensionPropertiesList.get(0).extensionNameString()))
                    {
                        System.out.printf("Enabling device extension: %s\n", ext);
                        return true;
                    }
                }
                return false;
            });
        }
    }

    public static boolean isSuitableQueueFamily(VkPhysicalDevice physicalDevice, final VkQueueFamilyProperties properties, int familyIndex, final QueueRequirements requirements)
    {
        if ((properties.queueFlags() & requirements.capacities) != 0)
        {
            if (requirements.pSurfaceSupport.length != 0)
            {
                try (ForeignStack stack = ForeignStack.pushConfined(Integer.BYTES))
                {
                    IntBuffer supportsPresentation = stack.mallocInt(1);
                    for (long surfaceSupport : requirements.pSurfaceSupport)
                    {
                        if (vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, familyIndex, surfaceSupport, supportsPresentation) != VK_SUCCESS)
                        {
                            System.err.println("An error occurred when processing surface support.");
                            return false;
                        }
                        if (supportsPresentation.get(0) == 0) return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private static final class PrototypedQueueBuilder
    {
        private int queueFamily, queueCount;
    }

    public static boolean selectAvailableQueues(VkPhysicalDevice physicalDevice, final QueueRequirements[] queueRequirementsList, List<Long> queueFinalIndexes, List<PrototypedQueueBuilder> generationMap)
    {
        try (ForeignStack stack = ForeignStack.pushConfined())
        {
            IntBuffer queueFamilyCount = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
            if (queueFamilyCount.get(0) == 0)
            {
                System.err.println("No queue family found on target device ?");
                return false;
            }
            VkQueueFamilyProperties.Buffer queueFamilyPropertiesList = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, queueFamilyPropertiesList);
            record QueueRequirements2(
                    QueueRequirements sub_ref,
                    int index
            ) {}
            List<QueueRequirements2> aggregateStack = new LinkedList<>();
            List<QueueRequirements2> strictStack = new LinkedList<>();
            int i;
            {
                queueFinalIndexes.clear();
                for (QueueRequirements req : queueRequirementsList)
                {
                    if (req.next != null)
                    {
                        aggregateStack.add(new QueueRequirements2(req, queueFinalIndexes.size()));
                        do {queueFinalIndexes.add(-1L);}
                        while ((req = req.next) != null);
                    }
                    else if (!req.strict_existence)
                    {
                        queueFinalIndexes.add(-1L);
                        aggregateStack.add(new QueueRequirements2(req, queueFinalIndexes.size() - 1));
                    }
                    else
                    {
                        queueFinalIndexes.add(-1L);
                        strictStack.add(new QueueRequirements2(req, queueFinalIndexes.size() - 1));
                    }
                }
            }
            early_step: for (i = 0; i < aggregateStack.size(); i++)
            {
                final QueueRequirements combined_requirements;
                int num_queues = 0;
                {
                    int capacities = 0;
                    List<Long> surface_mask = new LinkedList<>();
                    QueueRequirements current = queueRequirementsList[i];
                    while (current != null)
                    {
                        capacities |= current.capacities;
                        for (long surface_support : current.pSurfaceSupport)
                        {
                            surface_mask.add(surface_support);
                        }
                        current = current.next;
                        ++num_queues;
                    }
                    combined_requirements = new QueueRequirements(capacities, surface_mask.stream()
                            .mapToLong(Long::valueOf)
                            .toArray(), null, false);
                }

                for (PrototypedQueueBuilder gen_info : generationMap)
                {
                    final VkQueueFamilyProperties qp = queueFamilyPropertiesList.get(gen_info.queueFamily);
                    if (gen_info.queueCount + num_queues <= qp.queueCount() && isSuitableQueueFamily(physicalDevice, qp, gen_info.queueFamily, combined_requirements))
                    {
                        long family_and_index = gen_info.queueFamily;
                        for (int k = 0; k < num_queues; k++)
                        {
                            family_and_index &= ((long) (gen_info.queueCount + k) << 32) | 0xffffffffL;
                            queueFinalIndexes.set(aggregateStack.get(i).index + k, family_and_index);
                        }
                        gen_info.queueCount += num_queues;
                        continue early_step;
                    }
                }

                for (int j = 0; j < queueFamilyCount.get(0); j++)
                {
                    final VkQueueFamilyProperties qp = queueFamilyPropertiesList.get(j);
                    if (num_queues <= qp.queueCount() && isSuitableQueueFamily(physicalDevice, qp, j, combined_requirements))
                    {
                        PrototypedQueueBuilder builder = new PrototypedQueueBuilder();
                        builder.queueFamily = j;
                        builder.queueCount = num_queues;
                        generationMap.add(builder);
                        for (int k = 0; k < num_queues; k++)
                        {
                            queueFinalIndexes.set(aggregateStack.get(i).index + k, ((long) k << 32) | j);
                        }
                        continue early_step;
                    }
                }

                if (num_queues == 1)
                {
                    queueFinalIndexes.set(aggregateStack.get(i).index, -1L);
                    System.err.println("Could not find suitable family for lone queue.");
                }
                else
                {
                    aggregateStack.add(new QueueRequirements2(aggregateStack.get(i).sub_ref.next, aggregateStack.get(i).index + 1));
                }
            }
            found_p: for (final QueueRequirements2 strict_rq : strictStack)
            {
                PrototypedQueueBuilder genmap = null;
                for (int j = 0; j < queueFamilyCount.get(0); j++)
                {
                    if (isSuitableQueueFamily(physicalDevice, queueFamilyPropertiesList.get(j), j, strictStack.get(i).sub_ref))
                    {
                        genmap = null;
                        for (PrototypedQueueBuilder gp : generationMap)
                        {
                            if (gp.queueFamily == j)
                            {
                                genmap = gp;
                            }
                        }
                        if (genmap == null)
                        {
                            PrototypedQueueBuilder builder = new PrototypedQueueBuilder();
                            builder.queueFamily = j;
                            builder.queueCount = 1;
                            generationMap.add(builder);
                            queueFinalIndexes.set(strict_rq.index, (long)j);
                            continue found_p;
                        }
                    }
                }
                if (genmap != null)
                {
                    queueFinalIndexes.set(strict_rq.index, genmap.queueFamily | ((long)genmap.queueCount << 32));
                    genmap.queueCount += 1;
                }
                else
                {
                    queueFinalIndexes.set(strict_rq.index, -1L);
                    System.err.println("Could not find suitable family for lone queue.");
                }
            }
            return true;
        }
    }

    public int initialize(final VulkanContext.PhysicalDevice targetPhysicalDevice, final QueueRequirements[] queueRequirementsList, final String[] requiredExtensions, long psNext, VkPhysicalDeviceFeatures enabledFeatures)
    {
        assert queueRequirementsList.length != 0;
        if (this.m_internalHandle != null)
        {
            dispose();
        }

        List<Long> pQueueIndexes = new ArrayList<>();
        List<PrototypedQueueBuilder> generationMap = new ArrayList<>();
        if (!selectAvailableQueues(targetPhysicalDevice.handle(), queueRequirementsList, pQueueIndexes, generationMap))
        {
            return VK_ERROR_UNKNOWN;
        }

        this.m_enabledExtensions.addAll(filterDeviceExtensions(targetPhysicalDevice.handle(), Arrays.stream(requiredExtensions)).collect(Collectors.toUnmodifiableSet()));
        try (ForeignStack stack = ForeignStack.pushConfined())
        {
            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(stack);
            deviceCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            deviceCreateInfo.pNext(psNext);
            deviceCreateInfo.flags(0);
            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.malloc(generationMap.size(), stack);
            for (int i = 0; i < generationMap.size(); i++)
            {
                final PrototypedQueueBuilder builder = generationMap.get(i);
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.pNext(MemoryUtil.NULL);
                queueCreateInfo.flags(0);
                queueCreateInfo.queueFamilyIndex(builder.queueFamily);
                FloatBuffer pPriorities = stack.mallocFloat(builder.queueCount);
                MemoryUtil.memSet(pPriorities, 0x3f800000);
                queueCreateInfo.pQueuePriorities(pPriorities);
            }
            deviceCreateInfo.pQueueCreateInfos(queueCreateInfos);
            deviceCreateInfo.ppEnabledLayerNames(null);
            deviceCreateInfo.ppEnabledExtensionNames(stack.UTF8_list(this.m_enabledExtensions));
            deviceCreateInfo.pEnabledFeatures(enabledFeatures);
            PointerBuffer pVkDest = stack.mallocPointer(1);
            int vk_result = vkCreateDevice(targetPhysicalDevice.handle(), deviceCreateInfo, null, pVkDest);
            if (vk_result != VK_SUCCESS)
            {
                System.err.println("Vulkan device creation failed.");
                return vk_result;
            }
            this.m_internalHandle = new VkDevice(pVkDest.get(0), targetPhysicalDevice.handle(), deviceCreateInfo);

            for (long idx : pQueueIndexes)
            {
                int family = (int)(idx & 0xffffffffL);
                int index = (int)((idx >>> 32) & 0xffffffffL);
                vkGetDeviceQueue(this.m_internalHandle, family, index, pVkDest);
                this.m_generatedQueues.add(new QueueProperties(new VkQueue(pVkDest.get(0), this.m_internalHandle), family, index));
            }

            return VK_SUCCESS;
        }
    }

    public void dispose()
    {
        if (this.m_internalHandle == null) throw new IllegalStateException("VkDevice is null.");
        this.m_generatedQueues.clear();
        vkDestroyDevice(this.m_internalHandle, null);
        this.m_internalHandle = null;
        this.m_enabledExtensions.clear();
    }

    public VkDevice getHandle() {return this.m_internalHandle;}

}
