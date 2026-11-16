/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.rest.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.PetTypeMapper;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.controller.v1.PetTypeRestControllerV1;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeFieldsDto;
import org.springframework.samples.petclinic.service.ClinicService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PetTypeRestControllerV1TrimNameTests {

    @Mock
    private ClinicService clinicService;

    @Mock
    private PetTypeMapper petTypeMapper;

    private PetTypeRestControllerV1 controller() {
        return new PetTypeRestControllerV1(clinicService, petTypeMapper);
    }

    private void stubMapperForCreate() {
        when(petTypeMapper.toPetType(any(PetTypeFieldsDto.class))).thenAnswer(inv -> {
            PetTypeFieldsDto dto = inv.getArgument(0);
            PetType type = new PetType();
            type.setName(dto.getName());
            return type;
        });
        when(petTypeMapper.toPetTypeDto(any(PetType.class))).thenAnswer(inv -> {
            PetType type = inv.getArgument(0);
            PetTypeDto dto = new PetTypeDto();
            dto.setName(type.getName());
            return dto;
        });
    }

    private void stubSaveAssignsId() {
        doAnswer(inv -> {
            PetType type = inv.getArgument(0);
            type.setId(1);
            return null;
        }).when(clinicService).savePetType(any(PetType.class));
    }

    private ResponseEntity<PetTypeDto> create(String name) {
        PetTypeFieldsDto fields = new PetTypeFieldsDto();
        fields.setName(name);
        stubMapperForCreate();
        stubSaveAssignsId();
        return controller().addPetType(fields);
    }

    private String savedNameAfterCreate(String name) {
        create(name);
        ArgumentCaptor<PetType> captor = ArgumentCaptor.forClass(PetType.class);
        org.mockito.Mockito.verify(clinicService).savePetType(captor.capture());
        return captor.getValue().getName();
    }

    private String savedNameAfterUpdate(String submittedName) {
        PetType existing = new PetType();
        existing.setId(2);
        existing.setName("old-name");
        when(clinicService.findPetTypeById(2)).thenReturn(existing);
        when(petTypeMapper.toPetTypeDto(any(PetType.class))).thenAnswer(inv -> {
            PetType type = inv.getArgument(0);
            PetTypeDto dto = new PetTypeDto();
            dto.setName(type.getName());
            return dto;
        });

        PetTypeDto body = new PetTypeDto();
        body.setName(submittedName);
        ResponseEntity<PetTypeDto> response = controller().updatePetType(2, body);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());

        ArgumentCaptor<PetType> captor = ArgumentCaptor.forClass(PetType.class);
        org.mockito.Mockito.verify(clinicService).savePetType(captor.capture());
        return captor.getValue().getName();
    }

    @Test
    void testCreatePetTypeTrimsSurroundingSpaces() {
        ResponseEntity<PetTypeDto> response = create("  hamster  ");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("hamster", response.getBody().getName());
    }

    @Test
    void testCreatePetTypeSavesTrimmedName() {
        assertEquals("hamster", savedNameAfterCreate("  hamster  "));
    }

    @Test
    void testUpdatePetTypeTrimsSurroundingSpaces() {
        assertEquals("dog", savedNameAfterUpdate("  dog "));
    }

    @Test
    void testCreatePetTypeKeepsInternalSpaces() {
        ResponseEntity<PetTypeDto> response = create("guinea pig");

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("guinea pig", response.getBody().getName());
    }

    @Test
    void testUpdatePetTypeKeepsInternalSpaces() {
        assertEquals("guinea pig", savedNameAfterUpdate("  guinea pig  "));
    }

    @Test
    void testCreatePetTypeWithoutSurroundingSpacesUnchanged() {
        assertEquals("snake", savedNameAfterCreate("snake"));
    }

    @Test
    void testUpdatePetTypeWithoutSurroundingSpacesUnchanged() {
        assertEquals("snake", savedNameAfterUpdate("snake"));
    }

    @Test
    void testCreatePetTypeNullNamePassesThrough() {
        assertNull(savedNameAfterCreate(null));
    }

    @Test
    void testUpdatePetTypeNullNamePassesThrough() {
        assertNull(savedNameAfterUpdate(null));
    }
}
