package com.atlas.atlas_backend.notification;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendProvisionalPassword(String to, String provisionalPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Tu contraseña provisoria");
        message.setText("Hola,\n\nTu nueva contraseña provisoria es: " + provisionalPassword +
                "\n\nPor favor ingrese a la página y cambiela en 24 hs.\n\nSaludos cordiales,\nAtlas");
    }

    public void sendWelcomeAdminGlobal(String to, String provisionalPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bienvenido a Atlas - Administrador Global");
        message.setText("Hola,\n\nHas sido registrado como Administrador Global en el sistema Atlas.\n\n" +
                "Tu contraseña provisoria es: " + provisionalPassword +
                "\n\nPor favor, ingresa al sistema y cambia tu contraseña (tienes 24 horas antes de que expire).\n\nSaludos cordiales,\nAtlas");
        mailSender.send(message);
    }
}
