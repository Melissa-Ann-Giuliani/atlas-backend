package com.atlas.atlas_backend.notification;

import com.atlas.atlas_backend.cargo.Cargo;
import com.atlas.atlas_backend.funcion.Funcion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendProvisionalPassword(String to, String provisionalPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Tu contraseña provisoria");
        message.setText("Hola,\n\nTu nueva contraseña provisoria es: " + provisionalPassword +
                "\n\nPor favor ingrese a la página y cambiela en 24 hs.\n\nSaludos cordiales,\nAtlas");
        mailSender.send(message);
    }

    @Async
    public void sendWelcomeAdminGlobal(String to, String provisionalPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bienvenido a Atlas - Administrador Global");
        message.setText("Hola,\n\nHas sido registrado como Administrador Global en el sistema Atlas.\n\n" +
                "Tu contraseña provisoria es: " + provisionalPassword +
                "\n\nPor favor, ingresa al sistema y cambia tu contraseña (tienes 24 horas antes de que expire).\n\nSaludos cordiales,\nAtlas");
        mailSender.send(message);
    }

    @Async
    public void sendWelcomeAdminUnidad(String to, String provisionalPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bienvenido a Atlas - Administrador de Unidad");
        message.setText("Hola,\n\nHas sido registrado como Administrador de Unidad en el sistema Atlas.\n\n" +
                "Tu contraseña provisoria es: " + provisionalPassword +
                "\n\nPor favor, ingresa al sistema y cambia tu contraseña (tienes 24 horas antes de que expire).\n\nSaludos cordiales,\nAtlas");
        mailSender.send(message);
    }

    @Async
    public void sendWelcomeAuditor(String to, String provisionalPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bienvenido a Atlas - Auditor");
        message.setText("Hola,\n\nHas sido registrado como Auditor en el sistema Atlas.\n\n" +
                "Tu contraseña provisoria es: " + provisionalPassword +
                "\n\nPor favor, ingresa al sistema y cambia tu contraseña (tienes 24 horas antes de que expire).\n\nSaludos cordiales,\nAtlas");
        mailSender.send(message);
    }

    @Async
    public void sendWelcomeDocente(String to, String provisionalPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Bienvenido a Atlas - Docente");
        message.setText("Hola,\n\nHas sido registrado como Docente en el sistema Atlas.\n\n" +
                "Tu contraseña provisoria es: " + provisionalPassword +
                "\n\nPor favor, ingresa al sistema y cambia tu contraseña (tienes 24 horas antes de que expire).\n\nSaludos cordiales,\nAtlas");
        mailSender.send(message);
    }

    @Async
    public void sendDesignacionNotification(String to, String resolucion, LocalDate fechaInicio, LocalDate fechaFin) {
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Atlas - Notificación de Designación Aprobada");
            String vigenciaFin = (fechaFin != null) ? fechaFin.toString() : "Sin fecha de finalización definida";
            message.setText("Hola,\n\nSe ha registrado y aprobado tu designación docente en el sistema Atlas.\n\n" +
                    "Número de Resolución: " + resolucion + "\n" +
                    "Fecha de Inicio: " + fechaInicio + "\n" +
                    "Fecha de Finalización: " + vigenciaFin + "\n\n" +
                    "Tu estado en el sistema ahora se encuentra Activo.\n\nSaludos cordiales,\nAtlas");
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar notificación de designación a " + to + ": " + e.getMessage());
        }
    }

    @Async
    public void sendDesignacionCompletaNotification(String to, String resolucion, LocalDate fechaInicio,
            LocalDate fechaFin, Cargo cargo, List<Funcion> funciones) {
        if (to == null || to.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject("Atlas - Confirmación de Designación y Funciones Asignadas");
            String vigenciaFin = (fechaFin != null) ? fechaFin.toString() : "Sin fecha de finalización definida";
            StringBuilder body = new StringBuilder();
            body.append("Hola,\n\nSe ha completado y aprobado tu designación docente en el sistema Atlas.\n\n")
                    .append("Número de Resolución: ").append(resolucion).append("\n")
                    .append("Fecha de Inicio: ").append(fechaInicio).append("\n")
                    .append("Fecha de Finalización: ").append(vigenciaFin).append("\n");

            if (cargo != null) {
                body.append("Cargo Código: ").append(cargo.getCodigo()).append("\n");
                if (cargo.getUnidad() != null) {
                    body.append("Unidad Académica: ").append(cargo.getUnidad().getUnidadNombre()).append("\n");
                }
                if (cargo.getDedicacion() != null) {
                    body.append("Dedicación: ").append(cargo.getDedicacion().getDedicacionNombre())
                            .append(" (").append(cargo.getDedicacion().getDedicacionHoras()).append(" hs)\n");
                }
                body.append("Horas Ocupadas: ").append(cargo.getHorasOcupadas()).append(" hs\n");
            }

            if (funciones != null && !funciones.isEmpty()) {
                body.append("\nFunciones asignadas:\n");
                for (Funcion funcion : funciones) {
                    String materiaNombre = funcion.getMateria() != null ? funcion.getMateria().getNombre() : "-";
                    body.append("- ").append(funcion.getNombre())
                            .append(" [").append(funcion.getTipo()).append("]")
                            .append(" - Materia: ").append(materiaNombre)
                            .append(" (").append(funcion.getHoras()).append(" hs)\n");
                }
            }

            body.append("\nTu estado en el sistema ahora se encuentra Activo.\n\nSaludos cordiales,\nAtlas");
            message.setText(body.toString());
            mailSender.send(message);
        } catch (Exception e) {
            System.err.println("Error al enviar notificación de designación completa a " + to + ": " + e.getMessage());
        }
    }
}

    
    
        
    
