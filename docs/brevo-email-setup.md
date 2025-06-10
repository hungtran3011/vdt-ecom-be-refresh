# Brevo Email Configuration Guide

This document describes how to configure the application to use Brevo (formerly Sendinblue) for email services.

## Prerequisites

1. Create a Brevo account at [https://www.brevo.com/](https://www.brevo.com/)
2. Verify your domain or email address in Brevo
3. Generate an SMTP key in your Brevo account

## Getting Your Brevo SMTP Credentials

### Step 1: Create a Brevo Account
1. Sign up at [https://www.brevo.com/](https://www.brevo.com/)
2. Complete the account verification process

### Step 2: Generate SMTP Key
1. Log in to your Brevo dashboard
2. Go to **SMTP & API** → **SMTP**
3. Click **Generate a new SMTP key**
4. Give it a descriptive name (e.g., "ecom-app-smtp")
5. Copy the generated SMTP key - you'll need this for the `MAIL_PASSWORD` setting

### Step 3: Get Your Login Email
Your SMTP username will be the email address associated with your Brevo account.

## Configuration

Update your `.env` file with your Brevo credentials:

```bash
# Mail configuration for Spring Boot app (Brevo SMTP)
MAIL_HOST=smtp-relay.brevo.com
MAIL_PORT=587
MAIL_USERNAME=your-brevo-email@example.com
MAIL_PASSWORD=your-brevo-smtp-key
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_SSL=false
MAIL_SMTP_SSL_TRUST=smtp-relay.brevo.com
VDT_MAIL_FROM=your-brevo-email@example.com
VDT_MAIL_ENABLED=true
VDT_MAIL_ASYNC=true

# Keycloak SMTP Configuration (Brevo)
KC_SMTP_HOST=smtp-relay.brevo.com
KC_SMTP_PORT=587
KC_SMTP_FROM=your-brevo-email@example.com
KC_SMTP_USERNAME=your-brevo-email@example.com
KC_SMTP_PASSWORD=your-brevo-smtp-key
KC_SMTP_SSL=false
KC_SMTP_STARTTLS=true
```

## Important Notes

1. **Replace Placeholder Values**: 
   - Replace `your-brevo-email@example.com` with your actual Brevo account email
   - Replace `your-brevo-smtp-key` with the SMTP key generated in your Brevo dashboard

2. **Sender Email Verification**: 
   - Make sure the email address you use in `VDT_MAIL_FROM` and `KC_SMTP_FROM` is verified in your Brevo account
   - You can verify individual email addresses or entire domains

3. **Rate Limits**: 
   - Brevo free accounts have daily sending limits
   - Check your plan limits in the Brevo dashboard

4. **Security**:
   - Keep your SMTP key confidential
   - Never commit it to version control
   - Consider using environment-specific `.env` files

## Email Templates

The application uses the following email templates located in `src/main/resources/templates/`:
- `order-confirmation.html`
- `payment-success.html`
- `payment-failed.html`
- `refund-confirmation.html`
- `order-shipped.html`
- `order-delivered.html`

## Testing Email Configuration

To test your email configuration:

1. Start the application with the new Brevo configuration
2. Check the application logs for any SMTP connection errors
3. Trigger an email-sending action (e.g., place a test order)
4. Verify that emails are sent successfully through the Brevo dashboard

## Troubleshooting

### Common Issues:

1. **Authentication Failed**: 
   - Double-check your SMTP username and key
   - Ensure the SMTP key is active in your Brevo dashboard

2. **Sender Not Verified**: 
   - Verify your sender email address in Brevo
   - Use only verified email addresses in your configuration

3. **Rate Limit Exceeded**: 
   - Check your Brevo plan limits
   - Consider upgrading your plan if needed

4. **Connection Timeout**: 
   - Ensure port 587 is not blocked by your firewall
   - Try using alternative SMTP settings if available

### Brevo SMTP Settings:
- **Server**: smtp-relay.brevo.com
- **Port**: 587 (STARTTLS) or 465 (SSL)
- **Authentication**: Required
- **Encryption**: STARTTLS (recommended) or SSL

## Migration from Docker Mail Server

The application has been configured to use Brevo instead of the local Docker mail server. The following changes were made:

1. Updated SMTP configuration in `.env` and `mail.yml`
2. Commented out mail server services in `compose.yaml`
3. Removed mail server dependency from the main application service
4. Updated Keycloak SMTP configuration to use Brevo

The Docker mail server services are now disabled but left commented in the configuration for easy rollback if needed.
