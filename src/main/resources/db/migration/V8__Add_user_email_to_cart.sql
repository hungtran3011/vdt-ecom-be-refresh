-- Add user_email column to cart table for JWT integration
ALTER TABLE cart ADD COLUMN user_email VARCHAR(255);

-- Create index on user_email for performance
CREATE INDEX idx_cart_user_email ON cart(user_email);

-- Update existing carts to set user_email based on user_id if needed
-- (This is optional as we might not have existing data that needs migration)
