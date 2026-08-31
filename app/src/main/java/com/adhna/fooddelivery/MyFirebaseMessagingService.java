package com.adhna.fooddelivery;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    // علامة للتعرف على السجلات في Logcat
    private static final String TAG = "FCM_Service";
    
    // معرف قناة الإشعارات (ضروري لأندرويد 8.0+)
    private static final String CHANNEL_ID = "food_delivery_channel";
    
    // اسم القناة الذي سيظهر في إعدادات النظام
    private static final String CHANNEL_NAME = "إشعارات التطبيق";

    /**
     * يتم استدعاؤها عند استلام إشعار جديد من Firebase
     */
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "✅ تم استلام إشعار جديد!");

        // محاولة استخراج البيانات من الإشعار
        String title = "تطبيق التوصيل";
        String body = "لديك إشعار جديد";

        // التحقق إذا كان الإشعار يحتوي على بيانات نصية
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        }

        // استخراج بيانات إضافية (اختياري) من الـ Data Payload
        // مثلاً: String orderId = remoteMessage.getData().get("order_id");

        // عرض الإشعار في شريط الحالة
        sendNotification(title, body);
    }

    /**
     * يتم استدعاؤها عند تجديد رمز FCM (يحدث مرة عند التثبيت، وأحياناً عند تغيير الجهاز)
     */
    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "🆕 رمز FCM الجديد: " + token);

        // 🔴 خطوة مهمة جداً:
        // هنا يجب أن ترسل هذا الرمز (token) إلى السيرفر الخلفي الخاص بك (Backend)
        // حتى يتمكن السيرفر من إرسال الإشعارات لك باستخدام هذا الرمز.
        // مثلاً: sendTokenToServer(token);
    }

    /**
     * دالة مساعدة لبناء وعرض الإشعار في شريط الحالة
     */
    private void sendNotification(String title, String body) {
        // إنشاء Intent لفتح التطبيق عند الضغط على الإشعار
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        // إنشاء PendingIntent (يجب إضافة FLAG_IMMUTABLE لأندرويد 12+)
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        // بناء الإشعار
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.ic_dialog_info) // 🟡 غير الأيقونة لاحقاً
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true) // يختفي بعد الضغط عليه
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        // الحصول على مدير الإشعارات
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // ------------------------------------------------------
        // 🔥 مهم جداً: إنشاء قناة الإشعارات (لأندرويد 8.0 Oreo فأحدث)
        // بدون هذه القناة، لن تظهر الإشعارات على الأجهزة الحديثة!
        // ------------------------------------------------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            // إعدادات إضافية للقناة (اختياري)
            channel.setDescription("قناة استقبال إشعارات الطلبات والتحديثات");
            notificationManager.createNotificationChannel(channel);
        }

        // إظهار الإشعار (نستخدم الوقت الحالي كـ ID فريد)
        notificationManager.notify((int) System.currentTimeMillis(), notificationBuilder.build());
    }
}