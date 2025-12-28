"""
Setup configuration for IndCloud SDK.
"""
from setuptools import setup, find_packages
from pathlib import Path

# Read the README file
this_directory = Path(__file__).parent
long_description = (this_directory / "README.md").read_text(encoding="utf-8")

setup(
    name="indcloud-sdk",
    version="0.1.1",
    author="IndCloud Team",
    author_email="support@indcloud.io",
    description="Python SDK for IndCloud - The IoT platform that scales with you",
    long_description=long_description,
    long_description_content_type="text/markdown",
    url="https://github.com/CodeFleck/indcloud",
    project_urls={
        "Bug Tracker": "https://github.com/CodeFleck/indcloud/issues",
        "Documentation": "https://github.com/CodeFleck/indcloud/wiki",
        "Source Code": "https://github.com/CodeFleck/indcloud",
    },
    packages=find_packages(exclude=["tests", "tests.*", "examples"]),
    classifiers=[
        "Development Status :: 4 - Beta",
        "Intended Audience :: Developers",
        "Topic :: Software Development :: Libraries :: Python Modules",
        "Topic :: System :: Monitoring",
        "Topic :: System :: Hardware",
        "License :: OSI Approved :: MIT License",
        "Programming Language :: Python :: 3",
        "Programming Language :: Python :: 3.8",
        "Programming Language :: Python :: 3.9",
        "Programming Language :: Python :: 3.10",
        "Programming Language :: Python :: 3.11",
        "Programming Language :: Python :: 3.12",
        "Operating System :: OS Independent",
    ],
    python_requires=">=3.8",
    install_requires=[
        "requests>=2.25.0",
    ],
    extras_require={
        "async": ["aiohttp>=3.8.0"],
        "dev": [
            "pytest>=7.0.0",
            "pytest-asyncio>=0.21.0",
            "pytest-cov>=4.0.0",
            "mypy>=1.0.0",
            "black>=23.0.0",
            "flake8>=6.0.0",
            "isort>=5.12.0",
        ],
        "raspberry-pi": ["Adafruit-DHT>=1.4.0"],
    },
    keywords=[
        "iot",
        "telemetry",
        "monitoring",
        "sensor",
        "raspberry-pi",
        "esp32",
        "indcloud",
    ],
    include_package_data=True,
    zip_safe=False,
)
