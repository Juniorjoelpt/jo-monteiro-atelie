package br.com.mssolucoes.atelie.web;

import br.com.mssolucoes.atelie.servico.ArmazenamentoService;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Serve as fotos enviadas em /uploads/** e o frontend React (pasta static),
 * devolvendo index.html para as rotas do React (/admin, /admin/encomendas…).
 */
@Configuration
public class ConfiguracaoWeb implements WebMvcConfigurer {

    private final ArmazenamentoService armazenamento;

    public ConfiguracaoWeb(ArmazenamentoService armazenamento) {
        this.armazenamento = armazenamento;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(armazenamento.getPasta().toUri().toString())
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic());

        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic());

        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String caminho, Resource local) throws IOException {
                        Resource pedido = local.createRelative(caminho);
                        if (pedido.exists() && pedido.isReadable()) {
                            return pedido;
                        }
                        if (caminho.startsWith("api/") || caminho.startsWith("uploads/")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
