package net.cubizor.cubiloc

import net.cubizor.cubiloc.I18nTest.TestPlayer
import net.cubizor.cubiloc.I18nTest.TestPlayerLocaleProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class PlaceholderConcurrencyTest {

    @Test
    fun `concurrent renders never see another render's placeholders`() {
        val i18n = I18n(Locale.forLanguageTag("en-US"))
        i18n.registerLocaleProvider(TestPlayerLocaleProvider())
        i18n.loadMessages("messages", File("src/test/resources"))

        val threads = 8
        val rendersPerThread = 2_000
        val start = CountDownLatch(1)
        val failures = ConcurrentLinkedQueue<String>()

        val workers = (0 until threads).map { index ->
            thread {
                val name = "Player$index"
                val player = TestPlayer(name, "en_US")
                start.await()
                repeat(rendersPerThread) {
                    val result = i18n.context(player).use {
                        i18n.message("welcome").with("player" to name).asString()
                    }
                    if (!result.contains(name)) failures.add(result)
                }
            }
        }
        start.countDown()
        workers.forEach { it.join() }

        assertThat(failures).isEmpty()
    }
}
