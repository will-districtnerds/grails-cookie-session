/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Ben Lucchesi
 *  benlucchesi@gmail.com
 */

package grails.plugin.cookiesession;

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import grails.plugin.cookiesession.kryo.GrailsFlashScopeSerializer
import org.objenesis.strategy.StdInstantiatorStrategy
import com.esotericsoftware.kryo.serializers.FieldSerializer
import de.javakaffee.kryoserializers.*

//import org.codehaus.groovy.grails.web.servlet.GrailsFlashScope
import org.grails.web.servlet.GrailsFlashScope

import org.springframework.beans.factory.InitializingBean

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class KryoSessionSerializer implements SessionSerializer, InitializingBean{

  final static Logger log = LoggerFactory.getLogger(KryoSessionSerializer.class.getName());

  def grailsApplication

  boolean springSecurityCompatibility = false

  def springSecurityPluginVersion

  void afterPropertiesSet(){
      log.trace "bean properties set, performing bean configuring bean"

      if( grailsApplication.config.grails.plugin.cookiesession.containsKey('springsecuritycompatibility') ){
        springSecurityCompatibility = grailsApplication.config.grails.plugin.cookiesession['springsecuritycompatibility']?true:false
        springSecurityPluginVersion = grailsApplication.mainContext.getBean('pluginManager').allPlugins.find{ it.name == "springSecurityCore" }?.version
      }
      
      log.trace "Kryo serializer configured for spring security compatibility: ${springSecurityCompatibility}"
      if( springSecurityCompatibility ){
        log.trace "Kryo serializer detected spring security plugin version: ${springSecurityPluginVersion}"
      }
  }

  public byte[] serialize(SerializableSession session){
    log.trace "starting serialize session"

    Kryo kryo = getConfiguredKryoSerializer()
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream()
    Output output = new Output(outputStream)
    kryo.writeObject(output,session)
    output.close()
    def bytes = outputStream.toByteArray()
    log.trace "finished serializing session: ${bytes}"

    return bytes
  }

  public SerializableSession deserialize(byte[] serializedSession){
    log.trace "starting deserializing session"
    def input = new Input(new ByteArrayInputStream( serializedSession ) )
    Kryo kryo = getConfiguredKryoSerializer()
    SerializableSession session = kryo.readObject(input,SerializableSession.class)
    log.trace "finished deserializing session: ${session}"

    return session
  }

  private def getConfiguredKryoSerializer(){

    
    log.trace "configuring kryo serializer"

    def kryo = new Kryo()
    kryo.instantiatorStrategy = new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy())
    kryo.fieldSerializerConfig.setUseAsm(true)
    kryo.fieldSerializerConfig.setOptimizedGenerics(true)

    // register fieldserializer for GrailsFlashScope
    def flashScopeSerializer = new GrailsFlashScopeSerializer()
    kryo.register(GrailsFlashScope.class,flashScopeSerializer)
    log.trace "registered FlashScopeSerializer"

    def localeSerializer = new LocaleSerializer()
    kryo.register(java.util.Locale.class,localeSerializer)

    if( springSecurityCompatibility ){
      
      def grailsUserClass

      def usernamePasswordAuthenticationTokenClass = grailsApplication.classLoader.loadClass("org.springframework.security.authentication.UsernamePasswordAuthenticationToken")

      if( springSecurityPluginVersion[0].toInteger() >= 2 ){
        grailsUserClass = grailsApplication.classLoader.loadClass("grails.plugin.springsecurity.userdetails.GrailsUser")
        
        def simpleGrantedAuthorityClass = grailsApplication.classLoader.loadClass("org.springframework.security.core.authority.SimpleGrantedAuthority")
        def simpleGrantedAuthoritySerializer = new SimpleGrantedAuthoritySerializer()
        simpleGrantedAuthoritySerializer.targetClass = simpleGrantedAuthorityClass
        kryo.register(simpleGrantedAuthorityClass,simpleGrantedAuthoritySerializer)
        log.trace "registered SimpleGrantedAuthority serializer"
      }
      else{
        grailsUserClass = grailsApplication.classLoader.loadClass("org.codehaus.groovy.grails.plugins.springsecurity.GrailsUser")
      }

      def userClass = grailsApplication.classLoader.loadClass("org.springframework.security.core.userdetails.User")

      try {
        def grantedAuthorityImplClass = grailsApplication.classLoader.loadClass("org.springframework.security.core.authority.GrantedAuthorityImpl")
        def grantedAuthorityImplSerializer = new GrantedAuthorityImplSerializer()
        grantedAuthorityImplSerializer.targetClass = grantedAuthorityImplClass
        kryo.register(grantedAuthorityImplClass,grantedAuthorityImplSerializer)
        log.trace "registered GratedAuthorityImpl serializer"
      } catch (ClassNotFoundException e) {
        log.trace "GratedAuthorityImpl not found, no serializer registered"
      }

      def userSerializer = new UserSerializer()
      userSerializer.targetClass = userClass 
      kryo.register(userClass,userSerializer)
      log.trace "registered User serializer"

      def grailsUserSerializer = new GrailsUserSerializer()
      grailsUserSerializer.targetClass = grailsUserClass
      kryo.register(grailsUserClass,grailsUserSerializer)
      log.trace "registered GrailsUser serializer"

      def usernamePasswordAuthenticationTokenSerializer = new UsernamePasswordAuthenticationTokenSerializer()
      usernamePasswordAuthenticationTokenSerializer.targetClass = usernamePasswordAuthenticationTokenClass
      kryo.register(usernamePasswordAuthenticationTokenClass,usernamePasswordAuthenticationTokenSerializer)
      log.trace "registered UsernamePasswordAuthenticationToken serializer"
    }
    
    UnmodifiableCollectionsSerializer.registerSerializers( kryo );
    kryo.classLoader = grailsApplication.classLoader
    log.trace "grailsApplication.classLoader assigned to kryo.classLoader"

    kryo.register( Arrays.asList( "" ).getClass(), new ArraysAsListSerializer( ) );
    kryo.register( Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer() );
    kryo.register( Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer() );
    kryo.register( Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer() );
    kryo.register( Collections.singletonList( "" ).getClass(), new CollectionsSingletonListSerializer( ) );
    kryo.register( Collections.singleton( "" ).getClass(), new CollectionsSingletonSetSerializer( ) );
    kryo.register( Collections.singletonMap( "", "" ).getClass(), new CollectionsSingletonMapSerializer( ) );
    kryo.register( GregorianCalendar.class, new GregorianCalendarSerializer() );
    kryo.register( java.lang.reflect.InvocationHandler.class, new JdkProxySerializer( ) );

    SynchronizedCollectionsSerializer.registerSerializers( kryo );
    log.trace "configured kryo's standard serializers"

    return kryo
  }
}

public class LocaleSerializer extends Serializer<java.util.Locale> {

  final static Logger log = LoggerFactory.getLogger(LocaleSerializer.class.getName());

  public LocaleSerializer (){
  }

  @Override
  public void write (Kryo kryo, Output output, java.util.Locale locale) {
    log.trace "starting writing Locale: ${locale}"
    output.writeString(locale.language?:"")
    output.writeString(locale.country?:"")
    output.writeString(locale.variant?:"")
    log.trace "finished writing locale ${locale}"
  }

//  @Override
  public Locale create (Kryo kryo, Input input, Class<java.util.Locale> type) {
    log.trace "starting create Local"
    return read(kryo,input,type)
  }

  @Override
  public Locale read (Kryo kryo, Input input, Class<Locale> type) {
    log.trace "starting reading Locale"
    def locale = new java.util.Locale(input.readString(),input.readString(),input.readString()) 
    log.trace "finished reading Locale: ${locale}"
    return locale
  }
}

public class SimpleGrantedAuthoritySerializer extends Serializer<Object> {

  final static Logger log = LoggerFactory.getLogger(SimpleGrantedAuthoritySerializer.class.getName());
  def targetClass

  public SimpleGrantedAuthoritySerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object grantedAuth ) {
    log.trace "started writing SimpleGrantedAuthority ${grantedAuth}"
    kryo.writeClassAndObject( output, grantedAuth.role )
    log.trace "finished writing SimpleGrantedAuthority  ${grantedAuth}"
  }

//  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    log.trace "starting create SimpleGrantedAuthority" 
    return read(kryo,input,type)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    log.trace "starting reading SimpleGrantedAuthority"
    def role = kryo.readClassAndObject( input )
    
    def constructor = targetClass.getConstructor(String.class)
    def ga = constructor.newInstance(role)

    log.trace "finished reading SimpleGrantedAuthority: ${ga}"
    return ga
  }
}

public class GrantedAuthorityImplSerializer extends Serializer<Object> {

  final static Logger log = LoggerFactory.getLogger(GrantedAuthorityImplSerializer.class.getName());
  def targetClass

  public GrantedAuthorityImplSerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object grantedAuth ) {
    log.trace "started writing GrantedAuthorityImpl ${grantedAuth}"
    kryo.writeClassAndObject( output, grantedAuth.authority )
    log.trace "finished writing GrantedAuthorityImpl ${grantedAuth}"
  }

//  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    log.trace "starting create GrantedAuthorityImpl" 
    return read(kryo,input,type)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    log.trace "starting reading GrantedAuthorityImpl"
    def role = kryo.readClassAndObject( input )
    
    def constructor = targetClass.getConstructor(String.class)
    def ga = constructor.newInstance(role)

    log.trace "finished reading GrantedAuthorityImpl: ${ga}"
    return ga
  }
}

public class GrailsUserSerializer extends Serializer<Object> {

  final static Logger log = LoggerFactory.getLogger(GrailsUserSerializer.class.getName());
  def targetClass

  public GrailsUserSerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object user) {
    log.trace "starting writing ${user}"
    //NOTE: note writing authorities on purpose - those get written as part of the UsernamePasswordAuthenticationToken
    kryo.writeClassAndObject(output,user.id)
    kryo.writeClassAndObject(output,user.username)
    kryo.writeClassAndObject(output,user.accountNonExpired)
    kryo.writeClassAndObject(output,user.accountNonLocked)
    kryo.writeClassAndObject(output,user.credentialsNonExpired)
    kryo.writeClassAndObject(output,user.enabled)
    //kryo.writeClassAndObject(output,user.authorities)
    log.trace "finished writing ${user}"
  }

//  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    log.trace "creating GrailsUser"
    return read(kryo,input,type)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    log.trace "starting reading GrailsUser"
    def id = kryo.readClassAndObject( input )
    def username = kryo.readClassAndObject( input )
    def accountNonExpired = kryo.readClassAndObject( input )
    def accountNonLocked = kryo.readClassAndObject( input )
    def credentialsNonExpired = kryo.readClassAndObject( input )
    def enabled = kryo.readClassAndObject( input )
    def authorities = []
    def constructor = targetClass.getConstructor(String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, Collection.class, Object.class)
    def user = constructor.newInstance( username,
                               "",
                               enabled,
                               accountNonExpired,
                               credentialsNonExpired,
                               accountNonLocked,
                               authorities,
                               id )
    log.trace "finished reading ${user}"
    return user
  }
}

public class UserSerializer extends Serializer<Object>{

  // org.springframework.security.core.userdetails.User
  final static Logger log = LoggerFactory.getLogger(UserSerializer.class.getName());

  def targetClass

  public UserSerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object user) {
    log.trace "starting writing ${user}"
    //NOTE: note writing authorities on purpose - those get written as part of the UsernamePasswordAuthenticationToken
    kryo.writeClassAndObject(output,user.username)
    kryo.writeClassAndObject(output,user.isAccountNonExpired())
    kryo.writeClassAndObject(output,user.isAccountNonLocked())
    kryo.writeClassAndObject(output,user.isCredentialsNonExpired())
    kryo.writeClassAndObject(output,user.isEnabled())
    //kryo.writeClassAndObject(output,user.authorities)
    log.trace "finished writing ${user}"
  }

//  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    log.trace "creating GrailsUser"
    return read(kryo,input,type)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    log.trace "starting reading GrailsUser"
    def username = kryo.readClassAndObject( input )
    def accountNonExpired = kryo.readClassAndObject( input )
    def accountNonLocked = kryo.readClassAndObject( input )
    def credentialsNonExpired = kryo.readClassAndObject( input )
    def enabled = kryo.readClassAndObject( input )
    def authorities = []
    def constructor = targetClass.getConstructor(String.class, String.class, boolean.class, boolean.class, boolean.class, boolean.class, Collection.class)
    def user = constructor.newInstance( username,
                               "",
                               enabled,
                               accountNonExpired,
                               credentialsNonExpired,
                               accountNonLocked,
                               authorities )
    log.trace "finished reading ${user}"
    return user
  }
}

public class UsernamePasswordAuthenticationTokenSerializer extends Serializer<Object> {
  
  final static Logger log = LoggerFactory.getLogger(UsernamePasswordAuthenticationTokenSerializer.class.getName());
  def targetClass

  public UsernamePasswordAuthenticationTokenSerializer(){
  }

  @Override
  public void write (Kryo kryo, Output output, Object token) {
    log.trace "starting writing ${token}"
    kryo.writeClassAndObject(output,token.principal)
    kryo.writeClassAndObject(output,token.credentials)
    kryo.writeClassAndObject(output,token.authorities)
    log.trace "writing authorities: ${token.authorities.class.name} - ${token.authorities}"
    kryo.writeClassAndObject(output,token.details)
    log.trace "finsihed writing ${token}"
  }

//  @Override
  public Object create (Kryo kryo, Input input, Class<Object> type) {
    log.trace "creating UsernamePasswordAuthenticationToken"
    return read(kryo,input,type)
  }

  @Override
  public Object read (Kryo kryo, Input input, Class<Object> type) {
    log.trace "starting reading UsernamePasswordAuthenticationToken" 
    def principal = kryo.readClassAndObject( input )
    def credentials = kryo.readClassAndObject( input )
    def authorities = kryo.readClassAndObject( input )
    log.trace "Authorities: ${authorities}"
    if( authorities ){
      authorities.each{ log.trace "${it.class.name}, ${it}" }
    }
    def details = kryo.readClassAndObject( input )
    
    def constructor = targetClass.getConstructor(Object.class,Object.class,Collection.class)
    def token = constructor.newInstance(principal,credentials,authorities)
    token.details = details

    log.trace "finished reading UsernamePasswordAuthenticationToken ${token}"  

    return token
  }
}
