package com.frostyfox.ethosbackend.controller;

import com.frostyfox.ethosbackend.model.EthosModel;
import com.frostyfox.ethosbackend.service.EthosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EthosController {

    private final EthosService ethosService;

    @PostMapping("/api/ethos")
    public ResponseEntity<Object> triggerEthos(@RequestBody EthosModel ethosModel) {
        Object pythonResponse = ethosService.forwardToPython(ethosModel);
        return ResponseEntity.ok(pythonResponse);
    }

//    @PostMapping("/api/ethos")
//    public void sendEthos(
//            @RequestBody EthosModel ethosModel
//            ){
//        ethosService.sendEthos(ethosModel);
//    }

}
