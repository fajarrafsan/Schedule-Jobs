package com.contoh.test.dto;

import com.contoh.test.model.Status;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchJobsRequest {
    @NotBlank(message = "JobName is required")
    @Size(max = 100,message = "JobName Max 100 character")
    private String jobName;

    @Size(max = 50, message = "jobType max 500 character")
    private String jobType;

    private Status status;
}
