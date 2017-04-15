package uk.q3c.simplycd.agent.app

import com.google.inject.Inject
import com.google.inject.persist.PersistService
import org.slf4j.LoggerFactory
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.service.StopEvent

/**
 * Created by David Sowerby on 11 Apr 2017
 */
class PersistenceService @Inject constructor(val orientService: PersistService) : Service {
    private val log = LoggerFactory.getLogger(this.javaClass.name)

    override fun getName(): String {
        return "OrientDb Service"
    }

    override fun onStart(event: StartEvent?) {
        log.info("starting the Persistence Service: {}", name)
        orientService.start()
    }

    override fun onStop(event: StopEvent?) {
        log.info("stopping the Persistence Service: {}", name)
        orientService.stop()
    }
}