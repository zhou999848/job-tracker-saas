package com.example.jobtracker.dto;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO: 用于与前端通信的数据结构
 */
public class JobApplicationDto {

    @Id
    private UUID id= UUID.randomUUID();

   @NotBlank(message="company cannot be blank")
    private String company;
   @NotBlank(message="position cannot be blank")
   @Size(max=50,message="number<=50")
    private String position;
   @NotBlank(message="status cannot be blank")
    private String status;
   @NotNull(message="appliedDate cannot be null")
    private LocalDate appliedDate;

    // Getter & Setter 省略可用 Lombok（如 @Getter/@Setter）
   public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getAppliedDate() {
        return appliedDate;
    }

    public void setAppliedDate(LocalDate appliedDate) {
        this.appliedDate = appliedDate;
    }
}
