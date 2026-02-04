package com.frostyfox.ethosbackend.service;

import com.frostyfox.ethosbackend.model.EthosModel;
import com.frostyfox.ethosbackend.repository.EthosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EthosService {
    private final EthosRepository ethosRepository;

    private final WebClient webClient;

    public String getEthos(EthosModel ethosModel){
        return ethosRepository.save(ethosModel).getPackageDescription();
    }

    public Object forwardToPython(EthosModel ethosModel) {

        Map<String, String> payload = Map.of(
                "description", ethosModel.getPackageDescription()
        );

        Object response = webClient.post()
                .uri("/ai/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
        
        log.info("Python API Response: {}", response);
        System.out.println("=== /api/ethos Response ===");
        System.out.println("Response: " + response);
        System.out.println("==========================");
        
        return response;
    }

//    public void sendEthos(EthosModel ethosModel){
//        webClient.post()
//                .uri("/ai/analyze")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(ethosModel)
//                .retrieve()
//                .toBodilessEntity() // ignore response
//                .block();
//    }
}
