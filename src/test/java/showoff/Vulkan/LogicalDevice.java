package showoff.Vulkan;

import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.Vma;
import org.lwjgl.util.vma.VmaAllocatorCreateInfo;
import org.lwjgl.util.vma.VmaVulkanFunctions;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import showoff.Disposable;
import showoff.FrameAllocator;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.KHRSurface.*;

public class LogicalDevice implements Disposable
{
    public record QueueRequirements(
            Predicate<Integer> capacities,
            long[] pSurfaceSupport,
            QueueRequirements next,
            boolean strict_existence
    ) {}

    public record Queue(
            VkQueue handle,
            int family,
            int index
    ) {}

    private final VkDevice m_internalHandle;
    private final VulkanContext.PhysicalDevice m_physicalDevice;
    private final long m_vmaAllocator;
    public final List<String> m_enabledExtensions;
    public final List<Queue> generatedQueues;
    private final VulkanAllocator m_allocationHelper;

    private static Stream<String> filterDeviceExtensions(VkPhysicalDevice physicalDevice, Stream<String> targetExtensions) throws VulkanException
    {
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            IntBuffer extPropertiesListCount = allocator.mallocInt(1);
            VulkanException.check(vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer)null, extPropertiesListCount, null));
            VkExtensionProperties.Buffer extensionPropertiesList = VkExtensionProperties.malloc(extPropertiesListCount.get(0), allocator);
            VulkanException.check(vkEnumerateDeviceExtensionProperties(physicalDevice, (ByteBuffer)null, extPropertiesListCount, extensionPropertiesList));
            return targetExtensions.filter(ext ->
            {
                for (int j = 0; j < extPropertiesListCount.get(0); j++)
                {
                    if (ext.equals(extensionPropertiesList.get(j).extensionNameString()))
                    {
                        VulkanContext.gVulkanLogger.info("Enabling device extension: " + ext);
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private static boolean isSuitableQueueFamily(VkPhysicalDevice physicalDevice, final VkQueueFamilyProperties properties, int familyIndex, final QueueRequirements requirements)
    {
        if (requirements.capacities.test(properties.queueFlags()))
        {
            if (requirements.pSurfaceSupport.length > 0)
            {
                IntBuffer supportsPresentation = FrameAllocator.take().mallocInt(1);
                for (long surfaceSupport : requirements.pSurfaceSupport)
                {
                    if (vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, familyIndex, surfaceSupport, supportsPresentation) != VK_SUCCESS)
                    {
                        VulkanContext.gVulkanLogger.info("An error occurred when processing surface support.");
                        return false;
                    }
                    if (supportsPresentation.get(0) == 0) return false;
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

    private static void selectAvailableQueues(VkPhysicalDevice physicalDevice, final QueueRequirements[] queueRequirementsList, List<Long> queueFinalIndexes, List<PrototypedQueueBuilder> generationMap) throws VulkanException
    {
        if (queueRequirementsList.length == 0) return;
        FrameAllocator allocator = FrameAllocator.take();
        IntBuffer queueFamilyCount = allocator.mallocInt(1);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
        if (queueFamilyCount.get(0) == 0)
        {
            throw new VulkanException("No queue family found on target device ?");
        }
        VkQueueFamilyProperties.Buffer queueFamilyPropertiesList = VkQueueFamilyProperties.malloc(queueFamilyCount.get(0), allocator);
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
                Predicate<Integer> capacities = __ -> true;
                List<Long> surface_mask = new LinkedList<>();
                QueueRequirements current = queueRequirementsList[i];
                while (current != null)
                {
                    capacities = capacities.and(current.capacities);
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
                VulkanContext.gVulkanLogger.error("Could not find suitable family for lone queue.");
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
                if (isSuitableQueueFamily(physicalDevice, queueFamilyPropertiesList.get(j), j, strict_rq.sub_ref))
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
                VulkanContext.gVulkanLogger.error("Could not find suitable family for lone queue.");
            }
        }
    }

    private static long create_vma_allocator(final VkDevice device)
    {
        FrameAllocator allocator = FrameAllocator.take();
        VmaVulkanFunctions vulkanFunctions = VmaVulkanFunctions.calloc(allocator)
                .vkGetPhysicalDeviceProperties(device.getPhysicalDevice().getCapabilities().vkGetPhysicalDeviceProperties)
                .vkGetPhysicalDeviceMemoryProperties(device.getPhysicalDevice().getCapabilities().vkGetPhysicalDeviceMemoryProperties)
                .vkAllocateMemory(device.getCapabilities().vkAllocateMemory)
                .vkFreeMemory(device.getCapabilities().vkFreeMemory)
                .vkMapMemory(device.getCapabilities().vkMapMemory)
                .vkUnmapMemory(device.getCapabilities().vkUnmapMemory)
                .vkFlushMappedMemoryRanges(device.getCapabilities().vkFlushMappedMemoryRanges)
                .vkInvalidateMappedMemoryRanges(device.getCapabilities().vkInvalidateMappedMemoryRanges)
                .vkBindBufferMemory(device.getCapabilities().vkBindBufferMemory)
                .vkBindImageMemory(device.getCapabilities().vkBindImageMemory)
                .vkGetBufferMemoryRequirements(device.getCapabilities().vkGetBufferMemoryRequirements)
                .vkGetImageMemoryRequirements(device.getCapabilities().vkGetImageMemoryRequirements)
                .vkCreateBuffer(device.getCapabilities().vkCreateBuffer)
                .vkDestroyBuffer(device.getCapabilities().vkDestroyBuffer)
                .vkCreateImage(device.getCapabilities().vkCreateImage)
                .vkDestroyImage(device.getCapabilities().vkDestroyImage)
                .vkGetBufferMemoryRequirements2KHR(device.getCapabilities().vkGetBufferMemoryRequirements2KHR)
                .vkGetImageMemoryRequirements2KHR(device.getCapabilities().vkGetImageMemoryRequirements2KHR)
                .vkBindBufferMemory2KHR(device.getCapabilities().vkBindBufferMemory2KHR)
                .vkBindImageMemory2KHR(device.getCapabilities().vkBindImageMemory2)
                .vkCmdCopyBuffer(device.getCapabilities().vkCmdCopyBuffer);

        VmaAllocatorCreateInfo allocatorCreateInfo = VmaAllocatorCreateInfo.calloc(allocator);
        allocatorCreateInfo.device(device);
        allocatorCreateInfo.physicalDevice(device.getPhysicalDevice());
        allocatorCreateInfo.instance(device.getPhysicalDevice().getInstance());
        allocatorCreateInfo.pVulkanFunctions(vulkanFunctions);

        PointerBuffer pAllocator = allocator.mallocPointer(1);
        return Vma.vmaCreateAllocator(allocatorCreateInfo, pAllocator) == VK_SUCCESS ? pAllocator.get(0) : VK_NULL_HANDLE;
    }

    public LogicalDevice(VulkanContext.PhysicalDevice physicalDevice, final QueueRequirements[] queueRequirementsList, final String[] requiredExtensions, long psNext, VkPhysicalDeviceFeatures enabledFeatures, boolean use_vma) throws VulkanException
    {
        List<Long> pQueueIndexes = new ArrayList<>();
        List<PrototypedQueueBuilder> generationMap = new ArrayList<>();
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            selectAvailableQueues(physicalDevice.handle(), queueRequirementsList, pQueueIndexes, generationMap);
            List<String> enabledExtensions = filterDeviceExtensions(physicalDevice.handle(), Arrays.stream(requiredExtensions)).toList();
            VkDeviceCreateInfo deviceCreateInfo = VkDeviceCreateInfo.calloc(allocator);
            deviceCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            deviceCreateInfo.pNext(psNext);
            deviceCreateInfo.flags(0);
            if (!generationMap.isEmpty())
            {
                VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.calloc(generationMap.size(), allocator);
                for (int i = 0; i < generationMap.size(); i++)
                {
                    final PrototypedQueueBuilder builder = generationMap.get(i);
                    FloatBuffer pPriorities = allocator.mallocFloat(builder.queueCount);
                    MemoryUtil.memSet(pPriorities, 0x3f800000);
                    queueCreateInfos.get(i)
                            .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                            .queueFamilyIndex(builder.queueFamily)
                            .pQueuePriorities(pPriorities);
                }
                deviceCreateInfo.pQueueCreateInfos(queueCreateInfos);
            }
            deviceCreateInfo.ppEnabledLayerNames(null);
            deviceCreateInfo.ppEnabledExtensionNames(allocator.pointers(enabledExtensions.stream().map(allocator::UTF8).toArray(ByteBuffer[]::new)));
            deviceCreateInfo.pEnabledFeatures(enabledFeatures);

            PointerBuffer pVkDest = allocator.mallocPointer(1);
            VulkanException.check(vkCreateDevice(physicalDevice.handle(), deviceCreateInfo, null, pVkDest), "Vulkan device creation failed.");
            this.m_internalHandle = new VkDevice(pVkDest.get(0), physicalDevice.handle(), deviceCreateInfo);
            this.m_enabledExtensions = enabledExtensions;
            this.m_physicalDevice = physicalDevice;

            this.generatedQueues = new ArrayList<>(pQueueIndexes.size());
            for (long idx : pQueueIndexes)
            {
                int family = (int)(idx & 0xffffffffL);
                int index = (int)((idx >>> 32) & 0xffffffffL);
                vkGetDeviceQueue(this.m_internalHandle, family, index, pVkDest);
                this.generatedQueues.add(new Queue(new VkQueue(pVkDest.get(0), this.m_internalHandle), family, index));
            }

            if (use_vma)
            {
                this.m_vmaAllocator = create_vma_allocator(this.m_internalHandle);
                if (this.m_vmaAllocator != VK_NULL_HANDLE)
                {
                    this.m_allocationHelper = new VmaAllocator(this);
                }
                else
                {
                    VulkanContext.gVulkanLogger.error("Failed to initialize VMA, fallback to default allocator.");
                    this.m_allocationHelper = new DefaultAllocator(this);
                }
            }
            else
            {
                this.m_vmaAllocator = VK_NULL_HANDLE;
                this.m_allocationHelper = new DefaultAllocator(this);
            }
        }
    }

    public VkDevice get()
    {
        return this.m_internalHandle;
    }

    public VulkanContext.PhysicalDevice getBoundPhysicalDevice()
    {
        return this.m_physicalDevice;
    }

    public long getVmaAllocator()
    {
        return this.m_vmaAllocator;
    }

    public VulkanAllocator getAllocationHelper()
    {
        return this.m_allocationHelper;
    }

    @Override
    public void dispose()
    {
        if (this.m_vmaAllocator != VK_NULL_HANDLE) Vma.vmaDestroyAllocator(this.m_vmaAllocator);
        vkDestroyDevice(this.m_internalHandle, null);
    }
}
