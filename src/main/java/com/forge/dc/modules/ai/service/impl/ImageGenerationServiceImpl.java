package com.forge.dc.modules.ai.service.impl;

import com.forge.dc.modules.ai.client.AgnesApiClient;
import com.forge.dc.modules.ai.dto.ImageToImageDTO;
import com.forge.dc.modules.ai.dto.TextToImageDTO;
import com.forge.dc.modules.ai.service.ImageGenerationService;
import com.forge.dc.modules.ai.vo.ImageGenerationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationServiceImpl implements ImageGenerationService {

    private final AgnesApiClient agnesApiClient;

    @Override
    public ImageGenerationVO textToImage(TextToImageDTO dto) throws java.io.IOException {
        AgnesApiClient.AgnesImageResponse response = agnesApiClient.generateImage(
                dto.getPrompt(), dto.getSize(), true);

        AgnesApiClient.ImageData imageData = response.getData().get(0);

        return ImageGenerationVO.builder()
                .imageUrl(imageData.getUrl())
                .revisedPrompt(imageData.getRevisedPrompt())
                .build();
    }

    @Override
    public ImageGenerationVO imageToImage(ImageToImageDTO dto) throws java.io.IOException {
        AgnesApiClient.AgnesImageResponse response = agnesApiClient.imageToImage(
                dto.getPrompt(), dto.getSize(), dto.getImages(), true);

        AgnesApiClient.ImageData imageData = response.getData().get(0);

        return ImageGenerationVO.builder()
                .imageUrl(imageData.getUrl())
                .revisedPrompt(imageData.getRevisedPrompt())
                .build();
    }
}
