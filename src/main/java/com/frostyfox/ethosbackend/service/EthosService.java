package com.frostyfox.ethosbackend.service;

import com.frostyfox.ethosbackend.model.EthosModel;
import com.frostyfox.ethosbackend.repository.EthosRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class EthosService {
    private final EthosRepository ethosRepository;

    private final WebClient webClient;

    public String getEthos(EthosModel ethosModel){
        return ethosRepository.save(ethosModel).getDescription();
    }

    public Object forwardToPython(EthosModel ethosModel) {

        Map<String, String> payload = Map.of(
                "description", ethosModel.getDescription()
        );

        return webClient.post()
                .uri("/ai/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Object.class)
                .block();
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
