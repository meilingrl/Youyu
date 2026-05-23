package com.youyu.backend.controller.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmitStudentVerificationRequest {

    @NotBlank(message = "学号不能为空")
    @Size(max = 64, message = "学号不能超过64个字符")
    private String studentNo;

    @NotBlank(message = "真实姓名不能为空")
    @Size(max = 64, message = "真实姓名不能超过64个字符")
    private String realName;

    @Size(max = 128, message = "学院不能超过128个字符")
    private String college;

    @Size(max = 128, message = "专业不能超过128个字符")
    private String major;

    @Size(max = 64, message = "年级不能超过64个字符")
    private String grade;

    @Size(max = 128, message = "校园邮箱不能超过128个字符")
    private String campusEmail;

    @Size(max = 32, message = "认证方式不能超过32个字符")
    private String verificationMethod = "manual_review";

    public String getStudentNo() {
        return studentNo;
    }

    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getMajor() {
        return major;
    }

    public void setMajor(String major) {
        this.major = major;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getCampusEmail() {
        return campusEmail;
    }

    public void setCampusEmail(String campusEmail) {
        this.campusEmail = campusEmail;
    }

    public String getVerificationMethod() {
        return verificationMethod;
    }

    public void setVerificationMethod(String verificationMethod) {
        this.verificationMethod = verificationMethod;
    }
}
