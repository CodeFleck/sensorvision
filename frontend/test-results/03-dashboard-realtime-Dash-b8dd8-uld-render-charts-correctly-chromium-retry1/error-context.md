# Page snapshot

```yaml
- generic [ref=e4]:
  - generic [ref=e5]:
    - heading "Sign in to SensorVision" [level=2] [ref=e6]
    - paragraph [ref=e7]:
      - text: Or
      - link "create a new account" [ref=e8] [cursor=pointer]:
        - /url: /register
  - generic [ref=e9]:
    - generic [ref=e11]:
      - img [ref=e13]
      - generic [ref=e15]:
        - heading "Login Failed" [level=3] [ref=e16]
        - generic [ref=e17]: Invalid username or password. Please check your credentials and try again.
    - generic [ref=e18]:
      - generic [ref=e19]:
        - generic [ref=e20]: Username
        - textbox "Username" [ref=e21]: admin
      - generic [ref=e22]:
        - generic [ref=e23]: Password
        - textbox "Password" [ref=e24]: admin123
    - button "Sign in" [ref=e26] [cursor=pointer]
  - generic [ref=e31]: Or continue with
  - button "Sign in with Google" [ref=e33] [cursor=pointer]:
    - img [ref=e34]
    - text: Sign in with Google
  - link "Forgot your password?" [ref=e40] [cursor=pointer]:
    - /url: /forgot-password
```