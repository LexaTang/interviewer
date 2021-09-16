package interviewer

import graphql.GraphQL
import graphql.schema.DataFetchingEnvironment
import graphql.schema.idl.RuntimeWiring
import graphql.schema.idl.SchemaGenerator
import graphql.schema.idl.SchemaParser
import interviewer.gql.Hello
import io.vertx.core.http.HttpServerOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.graphql.*
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await

class MainVerticle : CoroutineVerticle() {
  private fun initData(): Iterable<Hello> {
    return (1..4).map { i -> Hello("Hello $i.", i) }
  }

  private fun hello(env: DataFetchingEnvironment): Iterable<String> {
    return initData().map { hello -> hello.word }
  }

  private fun setupGql(): GraphQL {
    val schema = "type Query { hello: [String] }"
    val schemaParser = SchemaParser()
    val typeDefinitionRegistry = schemaParser.parse(schema)
    val runtimeWiring = RuntimeWiring.newRuntimeWiring().type("Query") { builder ->
      builder.dataFetcher("hello", this::hello)
    }.build()
    val schemaGenerator = SchemaGenerator()
    val graphQLSchema = schemaGenerator.makeExecutableSchema(typeDefinitionRegistry, runtimeWiring)
    return GraphQL.newGraphQL(graphQLSchema).build()
  }

  override suspend fun start() {
    val gql = setupGql()
    println(gql.execute("{hello}"))
    val gqlOption = GraphQLHandlerOptions()
    val graphiQLHandlerOptions = GraphiQLHandlerOptions().setEnabled(true)
    val router = Router.router(vertx)
    router.route().handler(BodyHandler.create())
    router.route("/graphql").handler(ApolloWSHandler.create(gql))
    router.post("/graphql").handler(GraphQLHandler.create(gql))
    router.route("/graphiql/*").handler(GraphiQLHandler.create(graphiQLHandlerOptions))
    val http = vertx
      .createHttpServer(HttpServerOptions().addWebSocketSubProtocol("graphql-ws"))
      .requestHandler(router)
      .listen(8888).await()
    println(http.actualPort())
  }
}
