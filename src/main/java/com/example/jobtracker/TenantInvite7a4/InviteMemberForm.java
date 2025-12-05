package com.example.jobtracker.TenantInvite7a4;

import com.example.jobtracker.TenantInvite7a4.Role;

public class InviteMemberForm {


        private String email;
        private Role role;
        private Integer daysToExpire;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public Role getRole() {
            return role;
        }

        public void setRole(Role role) {
            this.role = role;
        }

        public Integer getDaysToExpire() {
            return daysToExpire;
        }

        public void setDaysToExpire(Integer daysToExpire) {
            this.daysToExpire = daysToExpire;
        }
    }



