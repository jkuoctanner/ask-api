import com.google.inject.AbstractModule
import com.google.inject.name.Names
import com.octanner.auth.OCTannerAuth
import com.octanner.auth.impl.{ OCTannerAuthImpl, SecretKey }
import play.api.{ Configuration, Environment }

/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.
 *
 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure() = {
    bindConstant().annotatedWith(Names.named("VictoriesBaseApiUrl")).to(getConfigValue("victories.base.api.url"))
    bindConstant().annotatedWith(Names.named("SolrSearchUrl")).to(getConfigValue("solr.search.url"))
    bindConstant.annotatedWith(classOf[SecretKey]).to(getConfigValue("token.hex.key"))
    bind(classOf[OCTannerAuth]).to(classOf[OCTannerAuthImpl])
  }

  protected def getConfigValue(key: String): String = {
    configuration.getString(key).getOrElse(throw new IllegalStateException(s"Need to provide $key in configuration"))
  }
}
