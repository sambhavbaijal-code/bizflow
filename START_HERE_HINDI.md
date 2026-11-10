# BizFlow ko kaise chalayein (Beginner Guide)

## Is project mein kya hai?

- `index.html` — website ka structure
- `styles.css` — website ka design
- `app.js` — buttons, forms, API calls aur frontend logic
- `src/main/java/` — Java Spring Boot backend
- `database/schema-mysql.sql` — MySQL database ka SQL structure
- `README.md` — complete technical guide aur API examples

## Login aur alag business workspace

Website kholkar **Create an account** chunein. Apna naam, email, kam se kam 8-character password aur pehli shop/firm ka naam bharein. Login ke baad header mein **＋ Business** dabakar doosri shop/firm ka naam likhein, phir dropdown se workspace badlein. Har business ke products, contacts, purchases aur sales alag rakhe jaate hain. Abhi staff invitations/team access nahi hai; har account apne workspaces ka owner hai.

## Mac par run karne ke steps

### Step 1: Java check karein

Terminal kholkar chalayein:

```bash
java -version
```

Java 21 recommended hai.

### Step 2: Maven install karein

Agar Homebrew installed hai:

```bash
brew install maven
```

Phir check karein:

```bash
mvn -version
```

### Step 3: Project folder open karein

ZIP extract karein. Terminal mein `bizflow` folder ke andar jaayein.

### Step 4: Application start karein

```bash
mvn spring-boot:run
```

Pehli baar Maven required libraries download karega, isliye internet connection chahiye.

### Step 5: Website open karein

Browser mein ye address kholein:

```text
http://localhost:8080
```

Demo products aur supplier/customer pehli baar database empty hone par automatically add honge.

## Barcode aur cash memo scan kaise karein

1. **Products & Stock → Add product** mein product ka naam, SKU, barcode, price aur stock save karein. Barcode khaali chhodna optional hai.
2. **Sales & Billing → New sale** ya **Purchases → Record purchase** kholein.
   App poochega ki GST lagana hai ya nahi. Haan karne par percentage daalein; subtotal, GST aur total app khud dikhayega. Within-state par CGST + SGST, interstate par IGST invoice mein aayega. Product ke hisaab se sahi rate verify karein; app GSTIN ya GST return file nahi karta.
3. Barcode ke liye USB scanner se input box mein scan karein (ya code type karke **Find product** dabayein). Product mein saved koi bhi custom code/SKU chalega; UPC-A/EAN-13 leading-zero format bhi match hota hai. Camera scanner ke formats browser par depend karte hain aur permission maangega.
4. Cash memo ke liye **Read cash memo** dabakar saaf photo chunein. OCR catalogue mein milne wale products, quantity aur price form mein suggest karega.
5. Save karne se pehle customer/supplier, product, quantity aur price ko khud check karein. OCR image ko BizFlow record nahi karta; record tabhi save hota hai jab aap form submit karte hain.

Pehli baar memo OCR chalane ke liye internet chahiye, kyunki Tesseract library aur English language model load honge. OCR galti kar sakta hai, khaaskar dhundhli photo ya non-English text mein.

## Important

Default setup H2 file database use karta hai, jo local project folder ke `data` folder mein save hota hai. MySQL par switch karne ke steps `README.md` mein hain.

Ye educational MVP starter hai. Login aur business ke hisaab se data alag karna add hai. Abhi team invitations/roles nahi hain; production use se pehle staff permissions, durable sessions, backups, GST/tax review, audit logs, returns, payment integration aur security hardening add karein.

## BharatCode API key set karna

1. [BharatCode account](https://bharatcode.ai/account) se API key banayein.
2. Project folder mein `.env.example` ki copy banakar uska naam `.env` rakhein.
3. `.env` mein `BHARATCODE_API_KEY=` ke baad apni key paste karein.
4. VS Code mein **BizFlow (local data)** launch profile chunein aur app restart karein.

Asli `.env` file GitHub par upload nahi hoti. Key ko chat, screenshot ya public repo mein share na karein.
