package com.reactor.springbootreactor;

import com.reactor.springbootreactor.model.Comentarios;
import com.reactor.springbootreactor.model.Usuario;
import com.reactor.springbootreactor.model.UsuarioComentarios;
import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

@SpringBootApplication
public class SpringBootReactorApplication implements CommandLineRunner {


    private static final Logger log = LoggerFactory.getLogger(SpringBootReactorApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootReactorApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        ejemploContraPresion();

    }

    public void ejemploContraPresion() {
        Flux.range(1, 10)
                .log()
                .subscribe(new Subscriber<Integer>() {
                    private Subscription s;
                    private Integer limit = 5;
                    private Integer consume = 0;

                    @Override
                    public void onSubscribe(Subscription s) {
                        this.s = s;
                        s.request(limit);
                    }

                    @Override
                    public void onNext(Integer t) {
                        log.info(t.toString());
                        consume++;
                        if (consume == limit) {
                            consume = 0;
                            s.request(limit);
                        }
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void ejemploIntervalFromCreate() {

        Flux.create(emitter -> {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                private Integer counter = 0;

                @Override
                public void run() {
                    emitter.next(++counter);
                    if (counter == 10) {
                        timer.cancel();
                        emitter.complete();
                    }
                    if (counter == 5) {
                        timer.cancel();
                        emitter.error(new InterruptedException("Error, se ha detenido el flux en 5!"));
                    }
                }
            }, 1000, 1000);
        })
                .subscribe(next -> log.info(next.toString()),
                        error -> log.error(error.getMessage()),
                        () -> log.info("Hemos terminado"));
    }

    public void ejemploIntervalInfinite() throws InterruptedException {

        CountDownLatch latch = new CountDownLatch(1);
        Flux.interval(Duration.ofSeconds(1))
                .doOnTerminate(latch::countDown)
                .flatMap(i -> {
                    if (i >= 5) {
                        return Flux.error(new InterruptedException("Solo hasta 5!"));
                    }
                    return Flux.just(i);
                })
                .map(i -> "Hola " + i)
                .retry(2)
                .subscribe(s -> log.info(s), e -> log.error(e.getMessage()));

        latch.await();
    }

    public void ejemploDelayElements() {

        Flux<Integer> rango = Flux.range(1, 12)
                .delayElements(Duration.ofSeconds(1))
                .doOnNext(i -> log.info(i.toString()));
        rango.blockLast();
    }

    public void ejemploInterval() {

        Flux<Integer> rango = Flux.range(1, 12);
        Flux<Long> retraso = Flux.interval(Duration.ofSeconds(1));

        rango.zipWith(retraso, (ra, re) -> ra)
                .doOnNext(i -> log.info(i.toString()))
                .blockLast();

    }

    public void ejemploZipWithRange() {

        Flux.just(1, 2, 3, 4)
                .map(i -> (i * 2))
                .zipWith(Flux.range(0, 4), (uno, dos) -> String.format("Primer Flux: %d, Segundo Flux: %d", uno, dos))
                .subscribe(texto -> log.info(texto));

    }

    public void ejemploUsuarioComentariosZipWith2() {

        Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));
        Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            comentarios.addComentario("Hola pepe, qué tal?");
            comentarios.addComentario("Mañana voy a la playa!");
            comentarios.addComentario("Esto tomando el curso de spring con reactor");
            return comentarios;
        });

        usuarioMono.zipWith(comentariosUsuarioMono).map(tuple -> {
            Usuario u = tuple.getT1();
            Comentarios c = tuple.getT2();
            return new UsuarioComentarios(u, c);
        }).subscribe(uc -> log.info(uc.toString()));

    }

    public void ejemploUsuarioComentariosZipWith() {

        Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));
        Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            comentarios.addComentario("Hola pepe, qué tal?");
            comentarios.addComentario("Mañana voy a la playa!");
            comentarios.addComentario("Esto tomando el curso de spring con reactor");
            return comentarios;
        });

        usuarioMono.zipWith(comentariosUsuarioMono, (u, c) -> new UsuarioComentarios(u, c)).subscribe(uc -> log.info(uc.toString()));

    }


    public void ejemploUsuarioComentariosFlatMap() {

        Mono<Usuario> usuarioMono = Mono.fromCallable(() -> new Usuario("John", "Doe"));
        Mono<Comentarios> comentariosUsuarioMono = Mono.fromCallable(() -> {
            Comentarios comentarios = new Comentarios();
            comentarios.addComentario("Hola pepe, qué tal?");
            comentarios.addComentario("Mañana voy a la playa!");
            comentarios.addComentario("Esto tomando el curso de spring con reactor");
            return comentarios;
        });

        usuarioMono.flatMap(u -> comentariosUsuarioMono.map(c -> new UsuarioComentarios(u, c))).subscribe(uc -> log.info(uc.toString()));

    }

    public void ejemploIterable() {

        List<String> usuariosList = new ArrayList<>();
        usuariosList.add("Andres Guzman");
        usuariosList.add("Pedro Fulano");
        usuariosList.add("Maria Fulana");
        usuariosList.add("Diego Sultano");
        usuariosList.add("Juan Megani");
        usuariosList.add("Bruce Lee");
        usuariosList.add("Bruce Willis");

        Flux<String> nombres = Flux.fromIterable(usuariosList);

        Flux<Usuario> usuarios = nombres
                .map(nombre -> new Usuario(nombre.split(StringUtils.SPACE)[0].toLowerCase(), nombre.split(StringUtils.SPACE)[1].toUpperCase()))
                .filter(usuario -> usuario.getNombre().equals("bruce")).doOnNext(usuario -> {
                    if (usuario == null) {
                        throw new RuntimeException("Nombres no pueden ser vacíos");
                    }
                    System.out.println(usuario.getNombre().concat(StringUtils.SPACE).concat(usuario.getApellido()));

                }).map(usuario -> {
                    String nombre = usuario.getNombre().toLowerCase();
                    usuario.setNombre(nombre);
                    return usuario;
                });

        usuarios.subscribe(e -> log.info(e.toString()), error -> log.error(error.getMessage()), () -> log.info("Ha finalizado la ejecución del observador con éxito!"));

    }

    public void ejemploFlatMap() {

        List<String> usuariosList = new ArrayList<>();
        usuariosList.add("Andres Guzman");
        usuariosList.add("Pedro Fulano");
        usuariosList.add("Maria Fulana");
        usuariosList.add("Diego Sultano");
        usuariosList.add("Juan Megani");
        usuariosList.add("Bruce Lee");
        usuariosList.add("Bruce Willis");

        Flux.fromIterable(usuariosList)
                .map(nombre -> new Usuario(nombre.split(StringUtils.SPACE)[0].toLowerCase(), nombre.split(StringUtils.SPACE)[1].toUpperCase()))
                .flatMap(usuario -> {
                    if (usuario.getNombre().equalsIgnoreCase("bruce")) {
                        return Mono.just(usuario);
                    } else {
                        return Mono.empty();
                    }
                }).map(usuario -> {
            String nombre = usuario.getNombre().toLowerCase();
            usuario.setNombre(nombre);
            return usuario;
        }).subscribe(u -> log.info(u.toString()));
    }

    public void ejemploToString() {

        List<Usuario> usuariosList = new ArrayList<>();
        usuariosList.add(new Usuario("Andres", "Guzman"));
        usuariosList.add(new Usuario("Pedro", "Fulano"));
        usuariosList.add(new Usuario("Maria", "Fulana"));
        usuariosList.add(new Usuario("Diego", "Sultano"));
        usuariosList.add(new Usuario("Juan", "Megani"));
        usuariosList.add(new Usuario("Bruce", "Lee"));
        usuariosList.add(new Usuario("Bruce", "Willis"));

        Flux.fromIterable(usuariosList).map(
                usuario -> usuario.getNombre().toUpperCase().concat(StringUtils.SPACE).concat(usuario.getApellido().toUpperCase()))
                .flatMap(nombre -> {
                    if (nombre.contains("bruce".toUpperCase())) {
                        return Mono.just(nombre);
                    } else {
                        return Mono.empty();
                    }
                }).map(String::toLowerCase).subscribe(u -> log.info(u.toString()));
    }

    public void ejemploCollectList() {

        List<Usuario> usuariosList = new ArrayList<>();
        usuariosList.add(new Usuario("Andres", "Guzman"));
        usuariosList.add(new Usuario("Pedro", "Fulano"));
        usuariosList.add(new Usuario("Maria", "Fulana"));
        usuariosList.add(new Usuario("Diego", "Sultano"));
        usuariosList.add(new Usuario("Juan", "Megani"));
        usuariosList.add(new Usuario("Bruce", "Lee"));
        usuariosList.add(new Usuario("Bruce", "Willis"));

        Flux.fromIterable(usuariosList).collectList().subscribe(lista -> {
            lista.forEach(item -> log.info(item.toString()));
        });
    }


}
