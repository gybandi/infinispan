package org.infinispan.commons;

import org.infinispan.commons.util.KeyValueWithPrevious;
import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.ProtoSchema;

/**
 * Interface used to initialise the global {@link org.infinispan.protostream.SerializationContext} using the specified Pojos,
 * and the generated proto files and marshallers.
 *
 * @author Ryan Emerson
 * @since 10.0
 */
@ProtoSchema(
      includeClasses = KeyValueWithPrevious.class,
      schemaFileName = "global.commons.proto",
      schemaFilePath = "proto/generated",
      schemaPackageName = "org.infinispan.global.commons",
      service = false
)
public interface GlobalContextInitializer extends SerializationContextInitializer {
   GlobalContextInitializer INSTANCE = new GlobalContextInitializerImpl();
}
