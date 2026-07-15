package com.code.back_end.service;

import com.code.back_end.entity.Occupant;
import com.code.back_end.entity.Stall;
import com.code.back_end.repository.OccupantRepository;
import com.code.back_end.repository.StallRepository;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class StallService {

    private final StallRepository repository;
    private final OccupantRepository occupantRepository;

    public StallService(
            StallRepository repository,
            OccupantRepository occupantRepository
    ) {
        this.repository = repository;
        this.occupantRepository = occupantRepository;
    }

    public List<Stall> findAll() {
        return repository.findAll();
    }

    public Optional<Stall> findById(Long id) {
        return repository.findById(id);
    }

    public Stall save(Stall stall) {

        if (
                stall.getOccupant() != null &&
                stall.getOccupant().getId() != null
        ) {

            Occupant occupant =
                    occupantRepository.findById(
                            stall.getOccupant().getId()
                    ).orElseThrow(() ->
                            new RuntimeException(
                                    "Occupant not found"
                            )
                    );

            stall.setOccupant(occupant);
                stall.setStatus("OCCUPIED");

        } else {

            stall.setOccupant(null);

            if (
                    stall.getStatus() == null ||
                    stall.getStatus().isEmpty()
            ) {
                stall.setStatus("AVAILABLE");
            }
        }

        return repository.save(stall);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
