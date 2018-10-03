package uk.q3c.kaytee.agent.i18n;

import com.google.inject.Inject;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.q3c.krail.i18n.EnumResourceBundle;
import uk.q3c.krail.i18n.I18NKey;
import uk.q3c.krail.i18n.clazz.ClassBundleControl;
import uk.q3c.krail.i18n.clazz.DefaultClassPatternDao;
import uk.q3c.krail.i18n.persist.PatternCacheKey;
import uk.q3c.krail.i18n.persist.clazz.ClassPatternDao;
import uk.q3c.krail.i18n.persist.clazz.ClassPatternSource;

import javax.annotation.Nonnull;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.ResourceBundle;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Created by David Sowerby on 12 Mar 2017
 */
public class LocalClassPatternDao implements ClassPatternDao {
    public static final String CONNECTION_URL = "Class based";
    private static final String NO_WRITE = "Class based I18NPatterns cannot be written at runtime";
    private static Logger log = LoggerFactory.getLogger(DefaultClassPatternDao.class);
    protected Class<? extends Annotation> source;
    private ClassBundleControl control;


    @Inject
    protected LocalClassPatternDao() {
        super();
        source = ClassPatternSource.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getWriteFile() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWriteFile(@Nonnull File writeFile) {
        throw new UnsupportedOperationException(NO_WRITE);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressFBWarnings("EXS_EXCEPTION_SOFTENING_NO_CHECKED")
    @Override
    public Object write(@Nonnull PatternCacheKey cacheKey, @Nonnull String value) {
        throw new UnsupportedOperationException(NO_WRITE);
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Optional<String> deleteValue(@Nonnull PatternCacheKey cacheKey) {
        throw new UnsupportedOperationException("Class based I18NPatterns cannot be deleted");
    }

    /**
     * {@inheritDoc}
     */
    @Nonnull
    @Override
    public Optional<String> getValue(@Nonnull PatternCacheKey cacheKey) {
        checkNotNull(cacheKey);
        // source is used to qualify the Option
        log.debug("getValue for cacheKey {}, source '{}', using control: {}", cacheKey, source);
        I18NKey key = cacheKey.getKey();
        String expandedBaseName = expandFromKey(key);
        try {
            ResourceBundle bundle = (ResourceBundle) Class.forName(expandedBaseName).newInstance();
            return Optional.of(getValue(bundle, cacheKey.getKeyAsEnum()));
        } catch (Exception e) {
            log.warn("returning empty value, as getValue() returned exception {} with message '{}'", e, e.getMessage());
            return Optional.empty();
        }
    }

    protected String getValue(@Nonnull ResourceBundle bundle, @Nonnull Enum<?> key) {
        EnumResourceBundle enumBundle = (EnumResourceBundle) bundle;
        //noinspection unchecked
        enumBundle.setKeyClass(key.getClass());
        enumBundle.load();
        //noinspection unchecked
        return enumBundle.getValue(key);
    }

    /**
     * Sets the paths for location of class.  The bundle base name is taken from {@link I18NKey#bundleName()}.
     * <p>
     *
     * @param sampleKey any key from the I18NKey class, to give access to bundleName()
     * @return a path constructed from the {@code sampleKey}
     */
    protected String expandFromKey(@Nonnull I18NKey sampleKey) {
        checkNotNull(sampleKey);
        String baseName = sampleKey.bundleName();
        String packageName = ClassUtils.getPackageCanonicalName(sampleKey.getClass());
        return packageName.isEmpty() ? baseName : packageName + '.' + baseName;
    }

    public String getSourceString() {
        return source.getSimpleName();
    }

    public ResourceBundle.Control getControl() {
        return control;
    }

    /**
     * Returns {@link DefaultClassPatternDao#CONNECTION_URL} as a connection url
     *
     * @return {@link DefaultClassPatternDao#CONNECTION_URL} as a connection url
     */
    @Override
    public String connectionUrl() {
        return LocalClassPatternDao.CONNECTION_URL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long count() {
        throw new UnsupportedOperationException("count is not available for class based patterns");
    }


}
