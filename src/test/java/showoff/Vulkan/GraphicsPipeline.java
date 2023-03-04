package showoff.Vulkan;

import org.lwjgl.vulkan.*;
import showoff.Either;
import showoff.FrameAllocator;

import javax.annotation.Nullable;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.vulkan.VK13.*;

public class GraphicsPipeline extends Pipeline
{
    public record VertexShaderStage(ShaderModule module, InputData[] inputDataList, Attribute[] attributes)
    {
        public record InputData(int size, boolean per_vertex) {}
        public record Attribute(int buffer_index, int format, int offset) {}
    }
    public record Description(VertexShaderStage vertex_shader, ShaderModule fragment_shader, @Nullable ShaderModule tessellation_control_shader, @Nullable ShaderModule tessellation_evaluation_shader, @Nullable ShaderModule geometry_shader,
                              PushConstants[] pushConstantsList, long[] descriptorSetLayouts, Either<VkViewport.Buffer, Integer> viewports, Either<VkRect2D.Buffer, Integer> scissors, int num_samples) {}

    private final long m_internalHandle;
    private final long m_layout;

    public GraphicsPipeline(VkDevice device, long renderPass, Description description) throws VulkanException
    {
        super(device);
        try (FrameAllocator allocator = FrameAllocator.takeAndPush())
        {
            List<ShaderModule> shader_stages = new ArrayList<>(5);
            shader_stages.add(description.vertex_shader.module);
            shader_stages.add(description.fragment_shader);
            if (description.tessellation_control_shader != null) shader_stages.add(description.tessellation_control_shader);
            if (description.tessellation_evaluation_shader != null) shader_stages.add(description.tessellation_evaluation_shader);
            if (description.geometry_shader != null) shader_stages.add(description.geometry_shader);

            List<Integer> dynamicStates = new LinkedList<>();
            VkPipelineShaderStageCreateInfo.Buffer pipelineShaderStageCreateInfos = VkPipelineShaderStageCreateInfo.calloc(shader_stages.size(), allocator);
            for (int i = 0; i < shader_stages.size(); i++)
            {
                final ShaderModule shaderInfo = shader_stages.get(i);
                pipelineShaderStageCreateInfos.get(i)
                        .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                        .stage(shaderInfo.getStage())
                        .module(shaderInfo.get())
                        .pName(shaderInfo.getEntryPoint());
            }

            VkPushConstantRange.Buffer pushConstantRanges = VkPushConstantRange.calloc(description.pushConstantsList.length, allocator);
            for (int i = 0; i < description.pushConstantsList.length; i++)
            {
                pushConstantRanges.get(i)
                        .offset(description.pushConstantsList[i].offset())
                        .size(description.pushConstantsList[i].size())
                        .stageFlags(description.pushConstantsList[i].stages());
            }

            VkPipelineVertexInputStateCreateInfo vertexInputStateCreateInfo = VkPipelineVertexInputStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            if (description.vertex_shader.inputDataList.length > 0)
            {
                VkVertexInputBindingDescription.Buffer bindings = VkVertexInputBindingDescription.calloc(description.vertex_shader.inputDataList.length, allocator);
                for (int i = 0; i < description.vertex_shader.inputDataList.length; i++)
                {
                    bindings.get(i).binding(i)
                            .stride(description.vertex_shader.inputDataList[i].size)
                            .inputRate(description.vertex_shader.inputDataList[i].per_vertex ? VK_VERTEX_INPUT_RATE_VERTEX : VK_VERTEX_INPUT_RATE_INSTANCE);
                }
                vertexInputStateCreateInfo.pVertexBindingDescriptions(bindings);
            }
            if (description.vertex_shader.attributes.length > 0)
            {
                VkVertexInputAttributeDescription.Buffer attributes = VkVertexInputAttributeDescription.calloc(description.vertex_shader.attributes.length, allocator);
                for (int i = 0; i < description.vertex_shader.attributes.length; i++)
                {
                    attributes.get(i).binding(description.vertex_shader.attributes[i].buffer_index)
                            .location(i)
                            .format(description.vertex_shader.attributes[i].format)
                            .offset(description.vertex_shader.attributes[i].offset);
                }
                vertexInputStateCreateInfo.pVertexAttributeDescriptions(attributes);
            }

            VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateCreateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            VkPipelineTessellationStateCreateInfo tessellationStateCreateInfo = VkPipelineTessellationStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_TESSELLATION_STATE_CREATE_INFO);

            VkPipelineViewportStateCreateInfo viewportStateCreateInfo = VkPipelineViewportStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            description.viewports.apply(viewportStateCreateInfo::pViewports, dn ->
            {
                dynamicStates.add(VK_DYNAMIC_STATE_VIEWPORT);
                viewportStateCreateInfo.viewportCount(dn);
            });
            description.scissors.apply(viewportStateCreateInfo::pScissors, dn ->
            {
                dynamicStates.add(VK_DYNAMIC_STATE_SCISSOR);
                viewportStateCreateInfo.scissorCount(dn);
            });

            VkPipelineRasterizationStateCreateInfo rasterizationStateCreateInfo = VkPipelineRasterizationStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .depthClampEnable(false)
                    .rasterizerDiscardEnable(false)
                    .polygonMode(VK_POLYGON_MODE_FILL)
                    .cullMode(VK_CULL_MODE_BACK_BIT)
                    .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                    .depthBiasEnable(false)
                    .lineWidth(1.f);

            VkPipelineMultisampleStateCreateInfo multisampleStateCreateInfo = VkPipelineMultisampleStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .rasterizationSamples(description.num_samples)
                    .sampleShadingEnable(true)
                    .minSampleShading(1.f)
                    .alphaToCoverageEnable(false)
                    .alphaToOneEnable(false);

            VkPipelineDepthStencilStateCreateInfo depthStencilStateCreateInfo = VkPipelineDepthStencilStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO)
                    .depthTestEnable(true)
                    .depthWriteEnable(true)
                    .depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
                    .depthBoundsTestEnable(false)
                    .minDepthBounds(0.f)
                    .maxDepthBounds(1.f);

            VkPipelineColorBlendStateCreateInfo colorBlendStateCreateInfo = VkPipelineColorBlendStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .logicOp(VK_LOGIC_OP_COPY)
                    .attachmentCount(1)
                    .blendConstants(0, 1.f)
                    .blendConstants(1, 1.f)
                    .blendConstants(2, 1.f)
                    .blendConstants(3, 1.f)
                    .pAttachments(VkPipelineColorBlendAttachmentState.calloc(1, allocator)
                            .apply(0, state -> state
                                    .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                                    .blendEnable(true)
                                    .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                                    .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                                    .colorBlendOp(VK_BLEND_OP_ADD)
                                    .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                                    .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                                    .alphaBlendOp(VK_BLEND_OP_ADD)));

            VkPipelineDynamicStateCreateInfo dynamicStateCreateInfo = VkPipelineDynamicStateCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                    .pDynamicStates(allocator.ints(dynamicStates.stream().mapToInt(Integer::intValue).toArray()));

            LongBuffer pVkDest = allocator.mallocLong(1);
            VkPipelineLayoutCreateInfo layoutCreateInfo = VkPipelineLayoutCreateInfo.calloc(allocator)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pPushConstantRanges(pushConstantRanges)
                    .pSetLayouts(description.descriptorSetLayouts.length > 0 ? allocator.longs(description.descriptorSetLayouts) : null);
            VulkanException.check(vkCreatePipelineLayout(this.device, layoutCreateInfo, null, pVkDest));
            this.m_layout = pVkDest.get(0);

            VkGraphicsPipelineCreateInfo.Buffer pGraphicsPipelineCreateInfos = VkGraphicsPipelineCreateInfo.calloc(1, allocator)
                    .apply(0, ci -> ci
                            .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                            .stageCount(pipelineShaderStageCreateInfos.capacity())
                            .pStages(pipelineShaderStageCreateInfos)
                            .pVertexInputState(vertexInputStateCreateInfo)
                            .pInputAssemblyState(inputAssemblyStateCreateInfo)
                            .pTessellationState(tessellationStateCreateInfo)
                            .pViewportState(viewportStateCreateInfo)
                            .pRasterizationState(rasterizationStateCreateInfo)
                            .pMultisampleState(multisampleStateCreateInfo)
                            .pDepthStencilState(depthStencilStateCreateInfo)
                            .pColorBlendState(colorBlendStateCreateInfo)
                            .pDynamicState(dynamicStateCreateInfo)
                            .layout(this.m_layout)
                            .renderPass(renderPass)
                            .subpass(0)
                            .basePipelineIndex(-1));
            try
            {
                VulkanException.check(vkCreateGraphicsPipelines(this.device, VK_NULL_HANDLE, pGraphicsPipelineCreateInfos, null, pVkDest));
            }
            catch (VulkanException e)
            {
                vkDestroyPipelineLayout(this.device, this.m_layout, null);
                throw e;
            }
            this.m_internalHandle = pVkDest.get(0);
        }
    }

    @Override
    public long get()
    {
        return this.m_internalHandle;
    }

    @Override
    public long getLayout()
    {
        return this.m_layout;
    }
}
