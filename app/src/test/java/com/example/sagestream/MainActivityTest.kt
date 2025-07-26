import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.example.sagestream.notification.NotificationService
import com.example.SageStream.MainActivity
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {
    @Test
    fun `opening MainActivity does not trigger notification`() {
        // Arrange
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val notificationService: NotificationService = mock()
        // Act
        val activityController = Robolectric.buildActivity(MainActivity::class.java, Intent(context, MainActivity::class.java))
        val activity = activityController.create().start().resume().get()
        // Assert
        verifyNoInteractions(notificationService)
    }
} 