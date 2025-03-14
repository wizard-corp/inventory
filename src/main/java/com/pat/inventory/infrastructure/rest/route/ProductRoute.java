package com.pat.inventory.infrastructure.rest.route;

import com.pat.inventory.application.usecase.product.ProductUseCase;
import com.pat.inventory.application.usecase.product.SnowflakeIdGenerator;
import com.pat.inventory.domain.entities.Product;
import com.pat.inventory.domain.shared.exceptions.DomainException;
import com.pat.inventory.infrastructure.mypostgres.ProductRepository;
import com.pat.inventory.infrastructure.myrabbitmq.RabbitmqProducer;
import com.pat.inventory.infrastructure.rest.dto.ProductRequest;
import com.pat.inventory.infrastructure.shared.exceptions.InfrastructureException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@CrossOrigin
@RestController
@RequestMapping("/product")
public class ProductRoute {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final ProductRepository productRepository;
    private final RabbitmqProducer rabbitmqProducer;

    public ProductRoute(ProductRepository productRepository, RabbitmqProducer rabbitmqProducer) {
        this.productRepository = productRepository;
        this.rabbitmqProducer = rabbitmqProducer;
    }

    @GetMapping("/id_strategy")
    public ResponseEntity<Integer> getIdStrategy(HttpServletRequest request) {
        return new ResponseEntity<>(Integer.valueOf(Product.getIdStrategy()), HttpStatus.OK);
    }

    @GetMapping("/id")
    public ResponseEntity<Long> getId(HttpServletRequest request) {
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(0, 0);
        return new ResponseEntity<>(idGenerator.generateId(), HttpStatus.OK);
    }

    @GetMapping("/default")
    public ResponseEntity<String> getDefaultValues(HttpServletRequest request) {
        return new ResponseEntity<>(Product.getDefaultValues(), HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<Product>> getAllProducts(HttpServletRequest request) {
        String writeUId = request.getHeader("writeUId");
        ProductUseCase productUseCase = new ProductUseCase(writeUId, this.productRepository, this.rabbitmqProducer);

        try {
            List<Product> products = productUseCase.findAll();

            if (products.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(products, HttpStatus.OK);
        } catch (InfrastructureException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(Collections.emptyList(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{identifier}")
    public ResponseEntity<Product> getProductById(HttpServletRequest request, @PathVariable String identifier) {
        String writeUId = request.getHeader("writeUId");
        ProductUseCase productUseCase = new ProductUseCase(writeUId, this.productRepository, this.rabbitmqProducer);

        try {
            Optional<Product> optionalProduct = productUseCase.findById(identifier);
            return optionalProduct.map(product -> new ResponseEntity<>(product, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
        } catch (InfrastructureException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteProduct(HttpServletRequest request, @PathVariable String identifier) {
        String writeUId = request.getHeader("writeUId");
        ProductUseCase productUseCase = new ProductUseCase(writeUId, this.productRepository, this.rabbitmqProducer);

        try {
            productUseCase.delete(identifier);
            return ResponseEntity.ok().build();
        } catch (InfrastructureException e) {
            log.error(e.getMessage());
            return ResponseEntity.noContent().build();
        }
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> newProduct(HttpServletRequest request, @RequestBody ProductRequest productRequest) {
        String writeUId = request.getHeader("writeUId");
        ProductUseCase productUseCase = new ProductUseCase(writeUId, this.productRepository, this.rabbitmqProducer);

        try {
            Product product = Product.create(
                    productRequest.productId(),
                    productRequest.description(),
                    productRequest.statusValue());
            productUseCase.create(product);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (DomainException | InfrastructureException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/{identifier}")
    @ResponseStatus(HttpStatus.OK)
    public ResponseEntity<Void> updateProduct(HttpServletRequest request, @PathVariable String identifier, @RequestBody ProductRequest productRequest) {
        String writeUId = request.getHeader("writeUId");
        ProductUseCase productUseCase = new ProductUseCase(writeUId, this.productRepository, this.rabbitmqProducer);

        try {
            Product product = Product.existing(
                    identifier,
                    productRequest.description(),
                    productRequest.statusValue());
            productUseCase.update(product);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (DomainException | InfrastructureException e) {
            log.error(e.getMessage());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}
