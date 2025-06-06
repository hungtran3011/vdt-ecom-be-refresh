This application will use keycloak to integrate the user auth

# Keycloak Instructions

This application will be the resource server for the project.
It will sync with the keycloak server to get the user information.
Also, as the PKCE flow is used for the front-end, this should use the jwt to verify the user.

There should be an smtp server for the keycloak and the resource server notification

# Keycloak Configuration
1. **Create a Realm**: Create a new realm in Keycloak for your application. Named `ecom`
2. **Create the Backend clients**: 
   - Go to the Clients section and create a new client.
   - Set the Client ID to `ecom-be`.
   - Set the Access Type to `confidential`.
   - Set the Valid Redirect URIs to `http://localhost:8080/*`.
    - Set the Web Origins to `http://localhost:8080`.
   - Set the Admin URL to `http://localhost:8080`.
3. **Create the Frontend clients**: 
   - Go to the Clients section and create a new client.
   - Set the Client ID to `ecom-fe`.
   - Set the Access Type to `public`.
   - Set the Valid Redirect URIs to `http://localhost:3000/*`.
   - Set the Web Origins to `http://localhost:3000`.
4. **Create the User**:
   - Go to the Users section and create a new user.
   - Set the Username to `testuser`.
   - Set the Email to `testuser@example.com`.
   - Set the First Name to `Test`.
   - Set the Last Name to `User`.
   - Set the Password to `password`.
   - Enable the user.

5. **Create the User Role**:
    - Go to the Roles section and create a new role.
    - The roles should be "ADMIN", "CUSTOMER" and "SELLER"
    - The default role for the user should be "CUSTOMER"

6. **Config Email for the Keycloak**:
   - Go to the Realm Settings section.
   - Click on the Email tab.
   - Set the SMTP server details (host, port, username, password).
   - Enable the email settings.

7. **Configure the Client Scopes**:
   - Go to the Client Scopes section.
   - Create a new client scope named `ecom-scope`.
   - Add the `email`, `profile`, and `roles` mappers to this scope.
   - Assign this scope to both `ecom-be` and `ecom-fe` clients.

# Keycloak Integration
- Use Keycloak in Docker, check the [Keycloak Docker documentation](https://www.keycloak.org/getting-started/getting-started-docker) for more details.
- I used the `compose.yml` file to run the keycloak server, check it out
- This resource server should be configured to use the Keycloak server for authentication and authorization.
- This resource server has the Profile modules for the user, so it should be synced with the Keycloak server.
- The resource server should verify the JWT token from Keycloak to authenticate the user.
- The resource server should use the Keycloak Admin REST API to manage users and roles.
- The resource server shouldn't handle the PKCE flow for the frontend client.
