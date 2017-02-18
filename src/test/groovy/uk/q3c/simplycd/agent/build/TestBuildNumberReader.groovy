package uk.q3c.simplycd.agent.build

import com.google.inject.Singleton
import org.jetbrains.annotations.NotNull
import uk.q3c.simplycd.build.BuildNumberReader

/**
 * Created by David Sowerby on 28 Jan 2017
 */
@Singleton
class TestBuildNumberReader implements BuildNumberReader {

    private Map<String, Integer> numbers = new HashMap<>()
    private final Object mapLock = new Object()


    @Override
    int nextBuildNumber(@NotNull String projectName) {

        synchronized (mapLock) {
            Integer counter = numbers.get(projectName)
            if (counter == null) {
                counter = 0
            }
            counter++
            numbers.put(projectName, counter)
            return numbers.get(projectName)
        }

    }
}
