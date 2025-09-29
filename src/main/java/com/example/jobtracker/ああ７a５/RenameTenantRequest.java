package com.example.jobtracker.ああ７a５;

import jakarta.validation.constraints.Size;

// DTO
public record RenameTenantRequest(
        @Size(min=1, max=64)
        String newName
) {
    public String getNewName() {
        return newName;
    }


}