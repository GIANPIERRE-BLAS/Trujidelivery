const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

exports.sendPushNotification = functions.firestore
    .document("notificaciones/{notificacionId}")
    .onCreate(async (snap, context) => {
      const notificacion = snap.data();
      const usuarioId = notificacion.usuario_id;
      const titulo = notificacion.titulo;
      const mensaje = notificacion.mensaje;

      // Obtener el token del usuario (debes almacenar los tokens en Firestore)
      const usuarioDoc = await admin.firestore()
          .collection("usuarios")
          .doc(usuarioId)
          .get();

      if (!usuarioDoc.exists) {
        console.log("Usuario no encontrado:", usuarioId);
        return null;
      }

      const token = usuarioDoc.data().fcmToken;

      if (!token) {
        console.log("No se encontró token para el usuario:", usuarioId);
        return null;
      }

      const payload = {
        notification: {
          title: titulo,
          body: mensaje,
        },
      };

      try {
        await admin.messaging().send({
          token: token,
          ...payload,
          android: {
            priority: "high",
          },
        });
        console.log("Notificación enviada con éxito a:", usuarioId);
      } catch (error) {
        console.error("Error al enviar notificación:", error);
      }

      return null;
    });
