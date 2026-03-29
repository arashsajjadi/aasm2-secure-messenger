from __future__ import annotations

import sys
from pathlib import Path

from PySide6.QtCore import Qt
from PySide6.QtGui import QAction, QClipboard
from PySide6.QtWidgets import (
    QApplication,
    QCheckBox,
    QFileDialog,
    QGridLayout,
    QHBoxLayout,
    QLabel,
    QLineEdit,
    QMainWindow,
    QMessageBox,
    QPushButton,
    QSplitter,
    QStatusBar,
    QTabWidget,
    QTextEdit,
    QVBoxLayout,
    QWidget,
)

from .crypto_core import Aasm2Error, decrypt_bytes, decrypt_text_from_base64, encrypt_bytes, encrypt_text_to_base64

APP_TITLE = "AASM2 Secure Messenger"


STYLE = """
QWidget {
    background: #101318;
    color: #e8edf2;
    font-family: Segoe UI, Inter, Arial;
    font-size: 13px;
}
QMainWindow {
    background: #101318;
}
QLineEdit, QTextEdit {
    background: #171c23;
    border: 1px solid #2d3744;
    border-radius: 10px;
    padding: 8px;
    selection-background-color: #3d7fff;
}
QPushButton {
    background: #276ef1;
    border: none;
    border-radius: 10px;
    padding: 10px 14px;
    font-weight: 600;
}
QPushButton:hover {
    background: #3a82ff;
}
QPushButton:pressed {
    background: #1d5bd0;
}
QTabWidget::pane {
    border: 1px solid #2d3744;
    border-radius: 12px;
    padding: 10px;
}
QTabBar::tab {
    background: #171c23;
    border: 1px solid #2d3744;
    padding: 8px 14px;
    border-top-left-radius: 10px;
    border-top-right-radius: 10px;
    margin-right: 4px;
}
QTabBar::tab:selected {
    background: #1c2430;
}
QStatusBar {
    background: #0d1117;
    border-top: 1px solid #2d3744;
}
QCheckBox {
    spacing: 8px;
}
"""


class TextTab(QWidget):
    def __init__(self, password_input: QLineEdit, set_status) -> None:
        super().__init__()
        self.password_input = password_input
        self.set_status = set_status
        self._build_ui()

    def _build_ui(self) -> None:
        layout = QVBoxLayout(self)

        splitter = QSplitter(Qt.Orientation.Horizontal)
        self.input_text = QTextEdit()
        self.output_text = QTextEdit()
        self.input_text.setPlaceholderText("Enter plaintext or Base64 encrypted text here")
        self.output_text.setPlaceholderText("Encrypted or decrypted output will appear here")
        splitter.addWidget(self.input_text)
        splitter.addWidget(self.output_text)
        splitter.setSizes([500, 500])
        layout.addWidget(splitter)

        buttons = QGridLayout()
        actions = [
            ("Encrypt Text", self.encrypt_text),
            ("Decrypt Text", self.decrypt_text),
            ("Copy Output", self.copy_output),
            ("Load Output To Input", self.load_output_to_input),
            ("Save Output", self.save_output),
            ("Clear", self.clear_all),
        ]
        for index, (label, handler) in enumerate(actions):
            button = QPushButton(label)
            button.clicked.connect(handler)
            buttons.addWidget(button, index // 3, index % 3)
        layout.addLayout(buttons)

    def _password(self) -> str:
        password = self.password_input.text()
        if not password:
            raise Aasm2Error("Please enter a password.")
        return password

    def encrypt_text(self) -> None:
        try:
            encrypted = encrypt_text_to_base64(self.input_text.toPlainText(), self._password())
            self.output_text.setPlainText(encrypted)
            self.set_status("Text encrypted.")
        except Exception as exc:
            QMessageBox.critical(self, APP_TITLE, str(exc))

    def decrypt_text(self) -> None:
        try:
            decrypted = decrypt_text_from_base64(self.input_text.toPlainText().strip(), self._password())
            self.output_text.setPlainText(decrypted)
            self.set_status("Text decrypted.")
        except Exception as exc:
            QMessageBox.critical(self, APP_TITLE, str(exc))

    def copy_output(self) -> None:
        text = self.output_text.toPlainText()
        if not text:
            QMessageBox.warning(self, APP_TITLE, "Output is empty.")
            return
        QApplication.clipboard().setText(text, QClipboard.Mode.Clipboard)
        self.set_status("Output copied to clipboard.")

    def load_output_to_input(self) -> None:
        self.input_text.setPlainText(self.output_text.toPlainText())
        self.set_status("Output moved to input.")

    def save_output(self) -> None:
        text = self.output_text.toPlainText()
        if not text:
            QMessageBox.warning(self, APP_TITLE, "Output is empty.")
            return
        path, _ = QFileDialog.getSaveFileName(self, "Save Output", "output.txt", "Text Files (*.txt);;All Files (*)")
        if not path:
            return
        Path(path).write_text(text, encoding="utf-8")
        self.set_status(f"Output saved to {path}")

    def clear_all(self) -> None:
        self.input_text.clear()
        self.output_text.clear()
        self.set_status("Text fields cleared.")


class FileTab(QWidget):
    def __init__(self, password_input: QLineEdit, set_status) -> None:
        super().__init__()
        self.password_input = password_input
        self.set_status = set_status
        self.selected_file: str | None = None
        self._build_ui()

    def _build_ui(self) -> None:
        layout = QVBoxLayout(self)

        self.file_label = QLabel("No file selected")
        self.file_label.setWordWrap(True)
        layout.addWidget(self.file_label)

        controls = QHBoxLayout()
        choose_button = QPushButton("Choose File")
        choose_button.clicked.connect(self.choose_file)
        encrypt_button = QPushButton("Encrypt Selected File")
        encrypt_button.clicked.connect(self.encrypt_file)
        decrypt_button = QPushButton("Decrypt Selected File")
        decrypt_button.clicked.connect(self.decrypt_file)

        controls.addWidget(choose_button)
        controls.addWidget(encrypt_button)
        controls.addWidget(decrypt_button)
        layout.addLayout(controls)
        layout.addStretch(1)

    def _password(self) -> str:
        password = self.password_input.text()
        if not password:
            raise Aasm2Error("Please enter a password.")
        return password

    def choose_file(self) -> None:
        path, _ = QFileDialog.getOpenFileName(self, "Choose File", "", "All Files (*)")
        if not path:
            return
        self.selected_file = path
        self.file_label.setText(path)
        self.set_status("File selected.")

    def encrypt_file(self) -> None:
        try:
            if not self.selected_file:
                raise Aasm2Error("Please choose a file first.")
            input_path = Path(self.selected_file)
            blob = encrypt_bytes(
                input_path.read_bytes(),
                self._password(),
                {"type": "file", "name": input_path.name},
            )
            default_name = f"{input_path.name}.aasm2"
            save_path, _ = QFileDialog.getSaveFileName(self, "Save Encrypted File", default_name, "AASM2 Files (*.aasm2);;All Files (*)")
            if not save_path:
                return
            Path(save_path).write_bytes(blob)
            self.set_status(f"Encrypted file saved to {save_path}")
            QMessageBox.information(self, APP_TITLE, f"Encrypted file saved to:\n{save_path}")
        except Exception as exc:
            QMessageBox.critical(self, APP_TITLE, str(exc))

    def decrypt_file(self) -> None:
        try:
            if not self.selected_file:
                raise Aasm2Error("Please choose an encrypted file first.")
            input_path = Path(self.selected_file)
            result = decrypt_bytes(input_path.read_bytes(), self._password())
            default_name = result.metadata.get("name", "decrypted_output.bin")
            save_path, _ = QFileDialog.getSaveFileName(self, "Save Decrypted File", default_name, "All Files (*)")
            if not save_path:
                return
            Path(save_path).write_bytes(result.plaintext)
            self.set_status(f"Decrypted file saved to {save_path}")
            QMessageBox.information(self, APP_TITLE, f"Decrypted file saved to:\n{save_path}")
        except Exception as exc:
            QMessageBox.critical(self, APP_TITLE, str(exc))


class MainWindow(QMainWindow):
    def __init__(self) -> None:
        super().__init__()
        self.setWindowTitle(APP_TITLE)
        self.resize(1200, 780)
        self.setMinimumSize(980, 680)
        self.setStyleSheet(STYLE)
        self._build_ui()

    def _build_ui(self) -> None:
        central = QWidget()
        layout = QVBoxLayout(central)
        layout.setContentsMargins(18, 18, 18, 18)
        layout.setSpacing(14)

        title = QLabel(APP_TITLE)
        title.setStyleSheet("font-size: 26px; font-weight: 700; color: #f5f7fa;")
        subtitle = QLabel("Cross platform AASM2 encryption for text and files. Compatible with the Android build in this bundle.")
        subtitle.setStyleSheet("color: #a8b3c2;")
        layout.addWidget(title)
        layout.addWidget(subtitle)

        password_row = QHBoxLayout()
        password_label = QLabel("Password")
        self.password_input = QLineEdit()
        self.password_input.setPlaceholderText("Enter encryption password")
        self.password_input.setEchoMode(QLineEdit.EchoMode.Password)
        self.password_input.setMinimumHeight(42)
        show_password = QCheckBox("Show")
        show_password.toggled.connect(self._toggle_password)
        password_row.addWidget(password_label)
        password_row.addWidget(self.password_input, 1)
        password_row.addWidget(show_password)
        layout.addLayout(password_row)

        tabs = QTabWidget()
        tabs.addTab(TextTab(self.password_input, self.set_status), "Text")
        tabs.addTab(FileTab(self.password_input, self.set_status), "Files")
        layout.addWidget(tabs, 1)

        self.setCentralWidget(central)
        self.status_bar = QStatusBar()
        self.status_bar.showMessage("Ready")
        self.setStatusBar(self.status_bar)

        quit_action = QAction("Quit", self)
        quit_action.triggered.connect(self.close)
        self.menuBar().addAction(quit_action)

    def _toggle_password(self, checked: bool) -> None:
        mode = QLineEdit.EchoMode.Normal if checked else QLineEdit.EchoMode.Password
        self.password_input.setEchoMode(mode)

    def set_status(self, message: str) -> None:
        self.status_bar.showMessage(message)


def main() -> int:
    app = QApplication(sys.argv)
    app.setApplicationName(APP_TITLE)
    window = MainWindow()
    window.show()
    return app.exec()


if __name__ == "__main__":
    raise SystemExit(main())
