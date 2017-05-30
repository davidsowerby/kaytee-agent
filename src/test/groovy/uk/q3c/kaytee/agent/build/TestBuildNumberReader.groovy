package uk.q3c.kaytee.agent.build

import com.google.inject.Singleton
import org.jetbrains.annotations.NotNull

/**
 * Created by David Sowerby on 28 Jan 2017
 */
@Singleton
class TestBuildNumberReader implements BuildNumberReader {

    private Map<String, Integer> numbers = new HashMap<>()
    private final Object mapLock = new Object()


    @Override
    String nextBuildNumber(@NotNull Build build) {

        synchronized (mapLock) {
            Integer counter = numbers.get(build.project.shortProjectName)
            if (counter == null) {
                counter = 0
            }
            counter++
            numbers.put(build.project.shortProjectName, counter)
            return numbers.get(build.project.shortProjectName)
        }

    }


}
