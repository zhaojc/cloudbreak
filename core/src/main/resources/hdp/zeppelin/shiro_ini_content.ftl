
[users]
# List of users with their password allowed to access Zeppelin.
# To use a different strategy (LDAP / Database / ...) check the shiro doc at http://shiro.apache.org/configuration.html#Configuration-INISections
admin = ${ zeppelin_admin_password }
#user1 = password2, role1, role2
#user2 = password3, role3
#user3 = password4, role2

# Sample LDAP configuration, for user Authentication, currently tested for single Realm
[main]
#ldapRealm = org.apache.shiro.realm.ldap.JndiLdapRealm
#ldapRealm.userDnTemplate = uid={0},cn=users,cn=accounts,dc=hortonworks,dc=com
#ldapRealm.contextFactory.url = ldap://ldaphost:389
#ldapRealm.contextFactory.authenticationMechanism = SIMPLE
sessionManager = org.apache.shiro.web.session.mgt.DefaultWebSessionManager
securityManager.sessionManager = $sessionManager
# 86,400,000 milliseconds = 24 hour
securityManager.sessionManager.globalSessionTimeout = 86400000
shiro.loginUrl = /api/login

[urls]
# anon means the access is anonymous.
# authcBasic means Basic Auth Security
# To enfore security, comment the line below and uncomment the next one
/api/version = anon
#/** = anon
/** = authc